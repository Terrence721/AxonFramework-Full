#!/usr/bin/env bash
#
# Starts a throwaway Oracle 23ai Free container for manually verifying Oracle-specific
# JDBC/DDL (see Oracle23aiUtils) against a real Oracle instance. This repo has no JDBC
# integration tests yet - common/src/test isn't converted - so this is currently the
# only way to check that kind of SQL actually works, not just that it's syntactically
# plausible.
#
# Not part of the build or CI - this is a local, manual verification tool only.
#
# Usage: ./scripts/oracle-test-up.sh

set -euo pipefail
cd "$(dirname "$0")/.."

MAX_ATTEMPTS=30
SLEEP_SECONDS=10

echo "==> Starting Oracle 23ai Free (first run pulls a multi-GB image and takes a few minutes to initialize)..."
docker compose up -d

echo "==> Waiting for the database to report healthy..."
attempt=1
while true; do
    health=$(docker inspect -f '{{.State.Health.Status}}' oracle23ai-test 2>/dev/null || echo "missing")
    if [ "$health" = "healthy" ]; then
        break
    fi
    if [ "$attempt" -gt "$MAX_ATTEMPTS" ]; then
        echo "==> ERROR: oracle23ai-test never reported healthy after $MAX_ATTEMPTS attempts."
        docker logs oracle23ai-test --tail 30
        exit 1
    fi
    echo "    - status: $health (attempt $attempt/$MAX_ATTEMPTS)..."
    sleep "$SLEEP_SECONDS"
    attempt=$((attempt + 1))
done

echo "==> Oracle 23ai Free is ready on localhost:1522 (user: system / TestPass123, PDB: FREEPDB1)."
echo "    Connect with sqlplus inside the container:"
echo "      docker exec -it oracle23ai-test sqlplus system/TestPass123@FREEPDB1"
