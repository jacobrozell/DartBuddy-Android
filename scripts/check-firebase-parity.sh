#!/usr/bin/env bash
# Compare Firebase Analytics / Crashlytics allowlists for this app's iOS pair.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ -n "${GITHUB_WORKSPACE:-}" ]]; then
  ROOT="$GITHUB_WORKSPACE"
else
  ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
fi

exec python3 "$SCRIPT_DIR/check-firebase-parity.py" --root "$ROOT" --pair "Dart Buddy" "$@"
