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

# 2. Module boundaries, mirroring the table in AGENTS.md.
# core/data owns persistence, the network and every external capability behind an interface.
ALLOWED = {
    "core/model": [],
    "core/data": ["core/model"],
    "core/ui": ["core/model"],
    "scan/api": ["core/model"],
    "scan/mlkit": ["scan/api", "core/model"],
    "capture": ["core/model", "core/data"],
    "feature/*": ["core/model", "core/data", "core/ui", "scan/api"],
    "widget": ["core/model", "core/data", "core/ui"],
    "app": ["core/*", "scan/*", "capture", "feature/*", "widget"],
}
# Both declaration forms: the typesafe accessor (projects.core.model) and the string path (project(":core:model")).
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
            errors.append(f"{rel}:{n}: Room belongs to core/data; read stored data through its repositories")
        for accessor, path in dep.findall(line):
            target = accessor.replace(".", "/") if accessor else path.strip(":").replace(":", "/")
            if target != module and not any(fnmatch.fnmatch(target, a) for a in ALLOWED[key]):
                errors.append(f"{rel}:{n}: {module} must not depend on {target}; the allowed list is in AGENTS.md")
for f in root.glob("*/**/src/main/kotlin/**/*.kt"):
    rel = str(f.relative_to(root))
    if "build" in f.parts or rel.startswith("core/data/"):
        continue
    for n, line in enumerate(f.read_text().splitlines(), 1):
        if re.search(r"\bandroidx\.room\.", line):
            errors.append(f"{rel}:{n}: Room belongs to core/data; read stored data through its repositories")
        if re.search(r"\bdev\.antomanc\.tameki\.core\.data\.db\b", line):
            errors.append(f"{rel}:{n}: DAOs and entities stay inside core/data; read stored data through its repositories")

# 3. No !! anywhere in production code: make the value non-null where it is known instead.
for f in sorted(root.glob("*/**/src/main/**/*.kt")):
    rel = str(f.relative_to(root))
    if "build" in f.parts:
        continue
    for n, line in enumerate(f.read_text().splitlines(), 1):
        if "!!" in line:
            errors.append(f"{rel}:{n}: `!!` is forbidden; make the value non-null where it is known (a local val, let, a non-null parameter)")

# 4. Money is a Long of minor units, never a binary float.
# core:model may use Double/Float only for quantities (files named *Quantity*) or with an explicit `// money-ok` line.
money = re.compile(r"\b(?:Double|Float)\b")
for f in sorted((root / "core/model/src/main").rglob("*.kt")):
    rel = str(f.relative_to(root))
    if fnmatch.fnmatch(f.stem, "*Quantity"):
        continue
    for n, line in enumerate(f.read_text().splitlines(), 1):
        if money.search(line) and "// money-ok" not in line:
            errors.append(f"{rel}:{n}: no Double/Float for money; use Money(minor: Long, currency). Quantities go in a *Quantity* file, other exceptions need `// money-ok`")

# 5. Wall-clock reads go through the injected Clock: a relative label or a screenshot that reads
# the system clock changes with today's date and cannot be tested at a fixed time.
clock_read = re.compile(r"\.now\(\s*\)")
millis = re.compile(r"\bSystem\.currentTimeMillis\(\)")
for f in sorted(root.glob("*/**/src/main/**/*.kt")):
    rel = str(f.relative_to(root))
    if "build" in f.parts:
        continue
    for n, line in enumerate(f.read_text().splitlines(), 1):
        code = line.split("//", 1)[0]
        if clock_read.search(code):
            errors.append(f"{rel}:{n}: read time through the injected Clock (`Instant.now(clock)`), not the system clock; a label that changes with today's date cannot be tested or screenshotted")
        if millis.search(code):
            errors.append(f"{rel}:{n}: read time through the injected Clock, not System.currentTimeMillis(); for a unique temp name use System.nanoTime()")

for e in errors:
    print(e, file=sys.stderr)
sys.exit(1 if errors else 0)
