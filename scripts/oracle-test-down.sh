#!/usr/bin/env bash
#
# Stops and removes the throwaway Oracle 23ai Free container and its volume - there is
# no persistent data worth keeping here, this is a scratch verification database only.
#
# Usage: ./scripts/oracle-test-down.sh

set -euo pipefail
cd "$(dirname "$0")/.."

docker compose down -v
echo "==> Oracle test container and volume removed."
