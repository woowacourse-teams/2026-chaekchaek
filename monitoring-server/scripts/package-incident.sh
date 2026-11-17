#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd "$(dirname "$0")/../.." && pwd)
revision=$(git -C "$repo_dir" rev-parse HEAD)
release_dir=${1:-"$repo_dir/monitoring-server/releases/$revision"}
mkdir -p "$release_dir"
(cd "$repo_dir/incident-analyzer" && ./gradlew bootJar)
cp "$repo_dir/incident-analyzer/build/libs/incident-analyzer.jar" "$release_dir/incident-analyzer.jar"
cp "$repo_dir/monitoring-server/incident/Dockerfile" "$release_dir/Dockerfile"
cp "$repo_dir/monitoring-server/incident/docker-compose.yml" "$release_dir/docker-compose.yml"
printf 'Release: %s\n' "$release_dir"
