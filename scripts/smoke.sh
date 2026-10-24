#!/usr/bin/env bash
# Safe smoke entry: launches its own backend against a new cfd_acceptance_* database.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "${PYTHON:-python3}" "$SCRIPT_DIR/run-acceptance.py" "$@"
