#!/usr/bin/env bash
#
# Polls a GitHub Actions workflow until it finishes running on the current HEAD, then prints its
# status/conclusion. Written to stop retyping the same "until [gh run list ...]; do sleep 5; done"
# incantation by hand for every commit during the file-by-file migration.
#
# Usage: ./scripts/wait-for-ci.sh <workflow-file> [repo]
#   ./scripts/wait-for-ci.sh build.yml
#   ./scripts/wait-for-ci.sh dependency-submission.yml Terrence721/AxonFramework-Full

set -euo pipefail

WORKFLOW="${1:?Usage: wait-for-ci.sh <workflow-file> [repo]}"
REPO="${2:-Terrence721/AxonFramework-Full}"

until [ "$(gh run list --repo "$REPO" --workflow "$WORKFLOW" --limit 1 --json status --jq '.[0].status')" = "completed" ]; do
    sleep 5
done

gh run list --repo "$REPO" --workflow "$WORKFLOW" --limit 1 --json headSha,status,conclusion --jq '.[0]'
