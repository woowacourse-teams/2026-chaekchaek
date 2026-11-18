#!/usr/bin/env bash
set -euo pipefail
release_dir=$(cd "${1:?release directory is required}" && pwd)
monitoring_dir=${MONITORING_DIR:-"$HOME/monitoring-server"}
export INCIDENT_ENV_FILE="$monitoring_dir/.env.incident"
revision=$(basename "$release_dir")
[[ "$revision" =~ ^[a-f0-9]{7,40}$ ]] || { echo '커밋 해시로 된 릴리스 디렉터리가 필요합니다' >&2; exit 1; }
export INCIDENT_IMAGE="chaekchaek-incident-analyzer:$revision"
[[ -f "$INCIDENT_ENV_FILE" ]] || { echo '.env.incident 파일이 필요합니다' >&2; exit 1; }
[[ -f "$release_dir/incident-analyzer.jar" ]] || { echo '빌드된 JAR 파일이 필요합니다' >&2; exit 1; }
docker network inspect chaekchaek-monitoring-server_default >/dev/null
compose=(docker compose --env-file "$INCIDENT_ENV_FILE" -f "$release_dir/docker-compose.yml")
"${compose[@]}" config --quiet
docker build --tag "$INCIDENT_IMAGE" "$release_dir"
"${compose[@]}" up -d --no-build --wait --wait-timeout 180 incident-analyzer
"${compose[@]}" ps
