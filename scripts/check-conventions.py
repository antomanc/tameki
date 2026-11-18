#!/usr/bin/env python3
"""Mechanical checks that Gradle and Android lint do not cover. Run from the repo root; exits 1 on a violation."""
import fnmatch
import pathlib
import re
import sys

root = pathlib.Path(__file__).resolve().parent.parent
errors = []

# 1. A string name defined in two modules with different text: at merge one silently replaces the other.
texts = {}  # name -> {(module, locale dir): text}
for f in root.glob("*/**/src/main/res/values*/strings.xml"):
    if "build" in f.parts:
        continue
    module = str(f.relative_to(root)).split("/src/")[0]
    for name, text in re.findall(r'<string name="([^"]+)"[^>]*>(.*?)</string>', f.read_text(), re.S):
        texts.setdefault(name, {})[(module, f.parent.name)] = text
for name, found in sorted(texts.items()):
    modules = {m for m, _ in found}
    differs = any(len({t for (m, loc), t in found.items() if loc == l}) > 1 for l in {loc for _, loc in found})
    if len(modules) > 1 and differs:
        errors.append(f'string "{name}" has different text in {", ".join(sorted(modules))}: give each a distinct name')

# 2. Intent extras are written and read only by AppRequest (core/ui).
extra = re.compile(r'"dev\.antomanc\.ameki\.[A-Z_]+"')
for f in root.glob("*/**/src/main/kotlin/**/*.kt"):
    if "build" in f.parts or f.name == "AppRequest.kt":
        continue
    for n, line in enumerate(f.read_text().splitlines(), 1):
        if extra.search(line):
            errors.append(f"{f.relative_to(root)}:{n}: Intent extra literal; go through AppRequest")

# 3. Module boundaries. Features and widgets get data from core/data, the only module that wires Sources.
ALLOWED = {
    "core/model": [],
    "core/ui": ["core/model"],
    "core/data": ["core/model", "source/*"],
    "source/api": ["core/model"],
    "source/*": ["source/api", "core/model"],
    "feature/*": ["core/*", "feature/*"],
    "widget": ["core/*"],
    "app": ["core/*", "feature/*", "widget"],
}
# Both declaration forms: the typesafe accessor (projects.source.google) and the string path (project(":source:google")).
dep = re.compile(r"\bprojects\.([a-zA-Z.]+)|\bproject\(\s*\"(:[\w:-]+)\"\s*\)")
for build in sorted(root.glob("**/*.gradle.kts")):
    module = str(build.parent.relative_to(root))
    if build.name == "settings.gradle.kts" or module == "." or module.startswith("build-logic") or "build" in build.parent.parts or ".gradle" in build.parts:
        continue
    key = module if module in ALLOWED else next((k for k in ALLOWED if fnmatch.fnmatch(module, k)), None)
    if key is None:
        errors.append(f"{module}: new module without a boundary; add it to ALLOWED in scripts/check-conventions.py")
        continue
    for n, line in enumerate(build.read_text().splitlines(), 1):
        rel = f"{module}/{build.name}"
        if module != "core/data" and "libs.room" in line:
            errors.append(f"{rel}:{n}: Room belongs to core/data; read stored data through its repositories (WidgetInstances, PlaceRepository)")
        for accessor, path in dep.findall(line):
            target = accessor.replace(".", "/") if accessor else path.strip(":").replace(":", "/")
            if target != module and not any(fnmatch.fnmatch(target, a) for a in ALLOWED[key]):
                errors.append(f"{rel}:{n}: {module} must not depend on {target}; features and widgets read weather through core/data, which owns the Sources")
            # core/data may use every Source, but api() would hand a concrete one to every module that uses core/data.
            elif module == "core/data" and target.startswith("source/") and target != "source/api" and re.search(r"\bapi\(", line):
                errors.append(f"{rel}:{n}: core/data exposes {target} with api(); use implementation() so features reach weather only through core/data")
for f in root.glob("*/**/src/main/kotlin/**/*.kt"):
    rel = str(f.relative_to(root))
    if "build" in f.parts or rel.startswith("core/data/"):
        continue
    for n, line in enumerate(f.read_text().splitlines(), 1):
        if re.search(r"\bandroidx\.room\.", line):
            errors.append(f"{rel}:{n}: Room belongs to core/data; read stored data through its repositories (WidgetInstances, PlaceRepository)")
        if re.search(r"\bdev\.antomanc\.ameki\.core\.data\.db\b", line):
            errors.append(f"{rel}:{n}: DAOs and entities stay inside core/data; read stored data through its repositories (WidgetInstances, PlaceRepository)")

# 4. Time comes from one place in UI code, so a screen or widget can be tested at any hour.
# Only the bodies of the owner functions below may read the clock, and each owner is defined once in its module,
# so neither a helper next to an owner nor a second function with the owner's name can hand out the clock.
clock = re.compile(r"\b(?:Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime)(?:\.now\(|::now\b)|System\.currentTimeMillis\b|Clock\.system")
clock_owners = {
    "core/ui": {"rememberWeatherTime"},  # the app's ticking now
    "widget": {"Render", "providePreview"},  # one now per widget update, and the picker preview
}


def defines(name, line):
    return re.search(r"\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?" + name + r"\s*\(", line)


def body_lines(lines, start):
    """Line numbers (1-based) of the function declared at lines[start], by brace depth; a one-line expression body is just that line."""
    if "{" not in lines[start]:
        return {start + 1}
    depth, seen = 0, False
    for i in range(start, len(lines)):
        for ch in lines[i]:
            if ch == "{":
                depth, seen = depth + 1, True
            elif ch == "}":
                depth -= 1
        if seen and depth <= 0:
            return set(range(start + 1, i + 2))
    return set(range(start + 1, len(lines) + 1))


definitions = {}  # (module, owner) -> [rel:line]
for f in sorted(root.glob("*/**/src/main/kotlin/**/*.kt")):
    rel = str(f.relative_to(root))
    if "build" in f.parts or not rel.startswith(("feature/", "widget/", "core/ui/", "app/")):
        continue
    module = rel.split("/src/")[0]
    lines = f.read_text().splitlines()
    owned = set()
    for i, line in enumerate(lines):
        for owner in clock_owners.get(module, ()):
            if defines(owner, line):
                definitions.setdefault((module, owner), []).append(f"{rel}:{i + 1}")
                owned |= body_lines(lines, i)
    for n, line in enumerate(lines, 1):
        if clock.search(line) and n not in owned:
            errors.append(f"{rel}:{n}: reads the clock; take now as a parameter, from rememberWeatherTime() in the app or from Render in widgets, so it can be tested at any hour")
for (module, owner), where in sorted(definitions.items()):
    if len(where) > 1:
        for w in where:
            errors.append(f"{w}: {owner} is defined {len(where)} times in {module}; only one {owner} may read the clock, so call it instead of adding another")

# 5. Effect helpers live in core/ui/effects, where effects are allowed because each helper has a test beside it
# that pins its keys, cancellation and cleanup.
effects = root / "core/ui/src/main/kotlin/dev/antomanc/ameki/core/ui/effects"
for f in sorted(effects.glob("*.kt")):
    test = root / "core/ui/src/test/kotlin/dev/antomanc/ameki/core/ui/effects" / (f.stem + "Test.kt")
    if not test.exists():
        errors.append(f"{f.relative_to(root)}: effect helper has no test at {test.relative_to(root)}; a helper that owns an effect needs a test for its keys, cancellation and cleanup")

# 6. Compose effects live in the tested helpers of core/ui/effects. Files listed here had them before the rule
# and may keep at most that many; lower a number when you remove one, never raise it.
effect = re.compile(r"\b(LaunchedEffect|DisposableEffect|SideEffect)\s*[({]")
EFFECTS_BEFORE = {
    "app/src/main/kotlin/dev/antomanc/ameki/MainActivity.kt": 1,
    "core/ui/src/main/kotlin/dev/antomanc/ameki/core/ui/components/Instrument.kt": 1,
    "feature/home/src/main/kotlin/dev/antomanc/ameki/feature/home/DayPage.kt": 5,
    "feature/home/src/main/kotlin/dev/antomanc/ameki/feature/home/HomeScreen.kt": 6,
    "feature/settings/src/main/kotlin/dev/antomanc/ameki/feature/settings/SettingsScreen.kt": 1,
}
# 7. No !! and no @Suppress in production code. Same rule for files that had them before.
hatch = re.compile(r"!!|@Suppress\(")
HATCHES_BEFORE = {
    "core/data/src/main/kotlin/dev/antomanc/ameki/core/data/WeatherRepository.kt": 1,
    "core/data/src/main/kotlin/dev/antomanc/ameki/core/data/location/DeviceLocation.kt": 1,
    "core/ui/src/main/kotlin/dev/antomanc/ameki/core/ui/ForecastView.kt": 1,
    "feature/home/src/main/kotlin/dev/antomanc/ameki/feature/home/Details.kt": 1,
    "feature/home/src/main/kotlin/dev/antomanc/ameki/feature/home/HomeScreen.kt": 1,
}
for f in sorted(root.glob("*/**/src/main/**/*.kt")):
    rel = str(f.relative_to(root))
    if "build" in f.parts:
        continue
    lines = f.read_text().splitlines()
    if not rel.startswith("core/ui/src/main/kotlin/dev/antomanc/ameki/core/ui/effects/"):
        found = [n for n, line in enumerate(lines, 1) for _ in effect.finditer(line)]
        if len(found) > EFFECTS_BEFORE.get(rel, 0):
            errors.append(f"{rel}:{found[-1]}: Compose effect outside core/ui/effects; decide in the ViewModel or a pure tested function (like nextStep) and apply the result with ApplyOnChange or another tested helper in core/ui/effects. Effects written inline in screens are where navigation bugs start.")
    found = [n for n, line in enumerate(lines, 1) for _ in hatch.finditer(line)]
    if len(found) > HATCHES_BEFORE.get(rel, 0):
        errors.append(f"{rel}:{found[-1]}: make the value non-null where it is known (a local val, let, a non-null parameter) instead of !!, and fix the warning instead of suppressing it")

for e in errors:
    print(e, file=sys.stderr)
sys.exit(1 if errors else 0)
