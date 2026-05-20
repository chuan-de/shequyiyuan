#!/usr/bin/env bash
set -euo pipefail

BASE_REF="${1:-origin/main}"
HEAD_REF="${2:-HEAD}"

changed_files=$(git diff --name-only "$BASE_REF" "$HEAD_REF" || true)

if [[ -z "$changed_files" ]]; then
  echo "No changed files detected between $BASE_REF and $HEAD_REF"
  exit 0
fi

blocked=()
while IFS= read -r file; do
  [[ -z "$file" ]] && continue

  case "$file" in
    backend-rewrite/*|server/legacy/*)
      case "$file" in
        backend-rewrite/README.md|backend-rewrite/ARCHIVE_ONLY.md|server/legacy/README.md)
          ;;
        *)
          blocked+=("$file")
          ;;
      esac
      ;;
  esac
done <<< "$changed_files"

if (( ${#blocked[@]} > 0 )); then
  echo "❌ Detected changes outside mainline business directories:"
  printf ' - %s\n' "${blocked[@]}"
  echo
  echo "Allowed mainline business roots:"
  echo " - server/backend-rewrite/"
  echo " - web/"
  echo
  echo "If this is intentional archival work, split it into dedicated non-business maintenance commit."
  exit 1
fi

echo "✅ Path guard passed: no non-mainline business changes detected."
echo "ℹ️ Reminder: runtime backend entry must be server/backend-rewrite/ (root backend-rewrite/ is archive-only)."
