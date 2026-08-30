#!/bin/bash

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
FRONTEND_DIR=$(cd "$SCRIPT_DIR/../../.." && pwd)

VERSION_FILE="$FRONTEND_DIR/VERSION"
HISTORY_FILE="$FRONTEND_DIR/deploy-history.log"

COMMIT_SHA=$(git rev-parse HEAD)
DEPLOYED_AT=$(date '+%Y-%m-%d %H:%M:%S')

# 현재 배포 버전
echo "$COMMIT_SHA" > "$VERSION_FILE"

# 기존 이력을 유지하면서 최신 배포를 맨 위에 추가
{
  echo "$DEPLOYED_AT | $COMMIT_SHA"
  [ -f "$HISTORY_FILE" ] && cat "$HISTORY_FILE"
} > "${HISTORY_FILE}.tmp"

mv "${HISTORY_FILE}.tmp" "$HISTORY_FILE"

echo "Deployment recorded"
echo "Version: $COMMIT_SHA"
echo "Deployed at: $DEPLOYED_AT"
