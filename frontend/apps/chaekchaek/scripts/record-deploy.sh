#!/bin/bash

set -euo pipefail

RECORD_DIR="${DEPLOY_RECORD_DIR:-/var/lib/chaekchaek-deploy}"
VERSION_FILE="$RECORD_DIR/VERSION"
HISTORY_FILE="$RECORD_DIR/deploy-history.log"

COMMIT_SHA="${GITHUB_SHA:-$(git rev-parse HEAD)}"
DEPLOYED_AT=$(date '+%Y-%m-%d %H:%M:%S')

# 동시 실행되는 workflow가 이력을 덮어쓰지 않도록 잠금
exec 9>"$RECORD_DIR/.record-deploy.lock"
flock 9

echo "$COMMIT_SHA" > "${VERSION_FILE}.tmp"
mv "${VERSION_FILE}.tmp" "$VERSION_FILE"

{
  echo "$DEPLOYED_AT | $COMMIT_SHA"
  [ -f "$HISTORY_FILE" ] && cat "$HISTORY_FILE"
} > "${HISTORY_FILE}.tmp"

mv "${HISTORY_FILE}.tmp" "$HISTORY_FILE"

echo "Deployment recorded"
echo "History file: $HISTORY_FILE"
echo "Version: $COMMIT_SHA"
echo "Deployed at: $DEPLOYED_AT"