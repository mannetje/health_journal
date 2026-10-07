#!/usr/bin/env bash
# Writes the code statistics and the unit test coverage to the GitHub job summary
# (or to stdout when run locally). Needs cloc and python3; both are optional.
set -u
cd "$(dirname "$0")/.."
out="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

{
  echo "## Code statistics"
  if command -v cloc >/dev/null 2>&1; then
    cloc --vcs=git --md --quiet 2>/dev/null | sed '/^cloc|/d'
  else
    echo "cloc is not installed"
  fi

  echo
  echo "## Unit test coverage (lines)"
  echo
  echo "| Module | Covered | Total | % |"
  echo "|---|---:|---:|---:|"
  python3 - <<'PY'
import os
import xml.etree.ElementTree as ET

reports = {
    "domain": "domain/build/reports/jacoco/test/jacocoTestReport.xml",
    "data": "data/build/reports/coverage/test/debug/report.xml",
    "app": "app/build/reports/coverage/test/debug/report.xml",
}
for name, path in reports.items():
    if not os.path.exists(path):
        print(f"| {name} | | | no report |")
        continue
    # The report-level LINE counter is a direct child of the root element
    line = next((c for c in ET.parse(path).getroot().findall("counter") if c.get("type") == "LINE"), None)
    if line is None:
        print(f"| {name} | | | no line counter |")
        continue
    covered, missed = int(line.get("covered")), int(line.get("missed"))
    total = covered + missed
    pct = f"{100 * covered / total:.1f}" if total else "n/a"
    print(f"| {name} | {covered} | {total} | {pct} |")
PY
} >> "$out"
