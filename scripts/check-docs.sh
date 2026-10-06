#!/usr/bin/env bash
# Checks the developer docs: relative markdown links must resolve, and source
# paths written in backticks in docs/dev must exist. Run from anywhere.
set -u
cd "$(dirname "$0")/.."

fail=0
files=$(find docs/dev docs/adr openspec/specs openspec/changes/archive -name '*.md'; echo CONTRIBUTING.md; echo README.md; echo CHANGELOG.md)

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

if [ "$fail" -eq 0 ]; then echo "docs ok"; fi
exit "$fail"
