#!/bin/sh
set -eu
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ "${1:-}" = verify ]; then
  exec python3 "$script_dir/design_guard.py" --job "$script_dir/../visual-contracts/chaekchaek.job.json" verify
fi
exec python3 "$script_dir/cases/chaekchaek.py" "$@"
