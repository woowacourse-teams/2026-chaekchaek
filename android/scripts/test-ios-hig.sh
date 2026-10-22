#!/bin/sh
set -eu

# The script must enter the host queue before building or touching a device.
queue="$HOME/.agents/skills/mobile-validation-queue/scripts/validation_queue.py"
if [ ! -f "$queue" ]; then
  echo "mobile-validation-queue 스킬을 먼저 설치하세요." >&2
  exit 2
fi
if ! python3 "$queue" check; then
  exec python3 "$queue" run --label "책췍 iOS HIG 검증" -- sh "$0" "$@"
fi

repo_root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
device_id="${1:-}"
started_device=false
original_content_size=""

if [ -z "$device_id" ]; then
  device_id="$(xcrun simctl list devices available -j | python3 -c '
import json, sys

devices = [
    device
    for entries in json.load(sys.stdin)["devices"].values()
    for device in entries
    if device["name"].startswith("iPhone") and device["state"] == "Shutdown"
]
selected = next(iter(devices), None)
if selected is None:
    raise SystemExit("소유권이 불명확한 실행 중 기기는 재사용하지 않습니다. 종료 상태의 iPhone Simulator가 필요합니다.")
print(selected["udid"])
')"
fi

device_state() {
  xcrun simctl list devices available -j | DEVICE_ID="$device_id" python3 -c '
import json, os
import sys
matches = [d for ds in json.load(sys.stdin)["devices"].values() for d in ds if d["udid"] == os.environ["DEVICE_ID"]]
if len(matches) != 1:
    raise SystemExit("지정한 Simulator를 확인할 수 없습니다.")
print(matches[0]["state"])
'
}
initial_state="$(device_state)"
if [ "$initial_state" != "Shutdown" ]; then
  echo "종료 상태가 아닌 Simulator는 자동 인수하지 않습니다: $device_id" >&2
  exit 2
fi

cleanup() {
  if [ -n "$original_content_size" ]; then
    xcrun simctl ui "$device_id" content_size "$original_content_size" || true
  fi
  if [ "$started_device" = true ]; then
    xcrun simctl shutdown "$device_id" || true
  fi
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM

workspace_key="$(printf '%s' "$repo_root" | shasum | cut -c1-12)"
derived_data="${TMPDIR:-/tmp}/ChaekchaekDerivedData-${workspace_key}"

cd "$repo_root"
xcodebuild build-for-testing \
  -project android/iosApp/iosApp.xcodeproj \
  -scheme Chaekchaek \
  -destination "platform=iOS Simulator,id=$device_id" \
  -only-testing:ChaekchaekUITests \
  -parallel-testing-enabled NO \
  -maximum-concurrent-test-simulator-destinations 1 \
  -derivedDataPath "$derived_data" \
  CODE_SIGNING_ALLOWED=NO


# A build must not silently claim a device that another process booted meanwhile.
current_state="$(device_state)"
if [ "$current_state" != "Shutdown" ]; then
  echo "빌드 중 Simulator 상태가 바뀌어 중단합니다: $device_id" >&2
  exit 2
fi
xcrun simctl boot "$device_id"
started_device=true

xcrun simctl bootstatus "$device_id" -b
original_content_size="$(xcrun simctl ui "$device_id" content_size)"

for content_size in large accessibility-extra-extra-extra-large; do
  xcrun simctl ui "$device_id" content_size "$content_size"
  xcodebuild test-without-building \
    -project android/iosApp/iosApp.xcodeproj \
    -scheme Chaekchaek \
    -destination "platform=iOS Simulator,id=$device_id" \
    -only-testing:ChaekchaekUITests \
    -parallel-testing-enabled NO \
    -maximum-concurrent-test-simulator-destinations 1 \
    -derivedDataPath "$derived_data" \
    CODE_SIGNING_ALLOWED=NO
done
