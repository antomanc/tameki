#!/usr/bin/env bash
# Build site/index.html and site/privacy.html from docs/privacy-policy.md.
set -euo pipefail
cd "$(dirname "$0")/.."

uv run --with markdown python3 - <<'PY'
from pathlib import Path
import html
import markdown
import re

md_path = Path("docs/privacy-policy.md")
site = Path("site")
site.mkdir(parents=True, exist_ok=True)
raw = md_path.read_text(encoding="utf-8")

# Drop the leading H1; the page title carries the brand.
body_md = re.sub(r"^#\s+.*\n+", "", raw, count=1)
# Promote locale H2 sections and keep the rest.
body_html = markdown.markdown(
    body_md,
    extensions=["sane_lists", "smarty"],
    output_format="html5",
)

css = """
:root {
  color-scheme: light dark;
  --paper: #F2F0EA;
  --ink: #1C1D1A;
  --muted: #5C5D58;
  --leaf: #2E7D4F;
  --rule: rgba(28, 29, 26, 0.12);
  --link: #1F6F5C;
}
@media (prefers-color-scheme: dark) {
  :root {
    --paper: #141513;
    --ink: #ECEAE3;
    --muted: #A8A79F;
    --leaf: #6FCF97;
    --rule: rgba(236, 234, 227, 0.14);
    --link: #6FCF97;
  }
}
* { box-sizing: border-box; }
html { font-size: 106%; }
body {
  margin: 0;
  min-height: 100vh;
  background:
    radial-gradient(1200px 600px at 10% -10%, rgba(31, 111, 92, 0.08), transparent 60%),
    var(--paper);
  color: var(--ink);
  font-family: "Iowan Old Style", "Palatino Linotype", Palatino, "Book Antiqua", Georgia, serif;
  line-height: 1.55;
}
header, main, footer {
  width: min(42rem, calc(100% - 2.5rem));
  margin-inline: auto;
}
header {
  padding: 2.75rem 0 1.25rem;
}
header a.brand {
  font-family: "Avenir Next", "Segoe UI", system-ui, sans-serif;
  font-weight: 300;
  font-size: 1.75rem;
  letter-spacing: -0.02em;
  color: var(--ink);
  text-decoration: none;
}
header p {
  margin: 0.6rem 0 0;
  color: var(--muted);
  font-size: 0.98rem;
}
main {
  padding-bottom: 3.5rem;
}
h2 {
  font-family: "Avenir Next", "Segoe UI", system-ui, sans-serif;
  font-weight: 500;
  font-size: 1.35rem;
  margin: 2.4rem 0 0.85rem;
  letter-spacing: -0.01em;
}
h3 {
  font-family: "Avenir Next", "Segoe UI", system-ui, sans-serif;
  font-weight: 500;
  font-size: 1.05rem;
  margin: 1.6rem 0 0.55rem;
}
p, li { color: var(--ink); }
p { margin: 0.65rem 0; }
ul { padding-left: 1.2rem; margin: 0.5rem 0 0.9rem; }
li { margin: 0.28rem 0; }
a { color: var(--link); }
hr {
  border: 0;
  border-top: 1px solid var(--rule);
  margin: 2.2rem 0;
}
footer {
  padding: 0 0 2.5rem;
  color: var(--muted);
  font-size: 0.9rem;
}
.skip {
  position: absolute;
  left: -999px;
}
.skip:focus {
  left: 1rem;
  top: 1rem;
  background: var(--paper);
  padding: 0.4rem 0.7rem;
  z-index: 2;
}
"""

privacy = f"""<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="light dark">
  <title>Tameki — Privacy policy</title>
  <meta name="description" content="Tameki privacy policy. Local-first expense tracking; no account, no server.">
  <style>{css}</style>
</head>
<body>
  <a class="skip" href="#content">Skip to content</a>
  <header>
    <a class="brand" href="index.html">Tameki</a>
    <p>Privacy policy · Last updated 18 November 2026</p>
  </header>
  <main id="content">
{body_html}
  </main>
  <footer>
    <p><a href="index.html">Home</a> · Contact: <a href="mailto:an.manc25@gmail.com">an.manc25@gmail.com</a></p>
  </footer>
</body>
</html>
"""

index = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta http-equiv="refresh" content="0; url=privacy.html">
  <meta name="color-scheme" content="light dark">
  <title>Tameki</title>
  <link rel="canonical" href="privacy.html">
  <style>
    :root { color-scheme: light dark; --paper:#F2F0EA; --ink:#1C1D1A; --link:#1F6F5C; }
    @media (prefers-color-scheme: dark) {
      :root { --paper:#141513; --ink:#ECEAE3; --link:#6FCF97; }
    }
    body {
      margin: 0; min-height: 100vh; display: grid; place-items: center;
      background: var(--paper); color: var(--ink);
      font-family: "Avenir Next", "Segoe UI", system-ui, sans-serif;
    }
    a { color: var(--link); }
  </style>
</head>
<body>
  <p><a href="privacy.html">Tameki privacy policy</a></p>
</body>
</html>
"""

(site / "privacy.html").write_text(privacy, encoding="utf-8")
(site / "index.html").write_text(index, encoding="utf-8")
print(f"Wrote {site / 'index.html'} and {site / 'privacy.html'}")
PY
