#!/usr/bin/env bash
# Checks the developer docs: relative markdown links must resolve, and source
# paths written in backticks in docs/dev must exist. Run from anywhere.
set -u
cd "$(dirname "$0")/.."

fail=0
files=$(find docs/dev docs/adr docs/backup-format openspec/specs openspec/playbooks openspec/changes/archive -name '*.md'; echo CONTRIBUTING.md; echo README.md; echo CHANGELOG.md; echo SECURITY.md; echo CODE_OF_CONDUCT.md; echo GOVERNANCE.md)

# 1. Relative links (everything a reader can reach; open changes are skipped on
#    purpose because they move to the archive when they ship)
for f in $files; do
  dir=$(dirname "$f")
  grep -oE '\]\([^)#]+(#[^)]*)?\)' "$f" | sed -E 's/^\]\(//; s/\)$//; s/#.*$//' | while read -r target; do
    case "$target" in
      http*|mailto:*|"") continue ;;
    esac
    if [ ! -e "$dir/$target" ]; then
      echo "BROKEN LINK in $f: $target"
      echo x > /tmp/check-docs-fail.$$
    fi
  done
done
[ -e /tmp/check-docs-fail.$$ ] && { rm -f /tmp/check-docs-fail.$$; fail=1; }

# 2. Backticked repo paths in docs/dev (full paths only, no ellipsis, no placeholders)
for f in $(find docs/dev -name '*.md'); do
  grep -oE '`(app|domain|data|docs|openspec|scripts|\.github)/[A-Za-z0-9_./-]+`' "$f" | tr -d '`' | sort -u | while read -r p; do
    if [ ! -e "$p" ]; then
      echo "MISSING PATH in $f: $p"
      echo x > /tmp/check-docs-fail.$$
    fi
  done
done
[ -e /tmp/check-docs-fail.$$ ] && { rm -f /tmp/check-docs-fail.$$; fail=1; }

# 3. Structured facts: the database version written in the docs and specs must be the one in the code
db_version=$(grep -oE '^[[:space:]]*version = [0-9]+,' data/src/main/java/nl/healthjournal/data/local/HealthJournalDatabase.kt | head -1 | grep -oE '[0-9]+')
if [ -z "$db_version" ]; then
  echo "CANNOT READ the database version from HealthJournalDatabase.kt"
  fail=1
else
  for f in $(find docs/dev openspec/specs -name '*.md'); do
    grep -noE '(Room v[0-9]+|schema v[0-9]+|schema version [0-9]+|database version [0-9]+|\(([0-9]+) now, next)' "$f" | while IFS=: read -r line match; do
      n=$(echo "$match" | grep -oE '[0-9]+' | head -1)
      if [ "$n" != "$db_version" ]; then
        echo "STALE DATABASE VERSION in $f:$line: '$match' (the code is at $db_version)"
        echo x > /tmp/check-docs-fail.$$
      fi
    done
  done
  [ -e /tmp/check-docs-fail.$$ ] && { rm -f /tmp/check-docs-fail.$$; fail=1; }
fi

# 4. Every spec in openspec/specs must be listed in the index
for spec in $(find openspec/specs -mindepth 2 -name spec.md | sed -e 's#^openspec/specs/##'); do
  if ! grep -qF "($spec)" openspec/specs/README.md; then
    echo "SPEC NOT IN INDEX: openspec/specs/$spec is missing from openspec/specs/README.md"
    fail=1
  fi
done

if [ "$fail" -eq 0 ]; then echo "docs ok"; fi
exit "$fail"
