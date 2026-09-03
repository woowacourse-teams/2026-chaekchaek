#!/bin/sh
set -eu

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
    if device["name"].startswith("iPhone")
]
selected = next((device for device in devices if device["state"] == "Booted"), None)
selected = selected or next(iter(devices), None)
if selected is None:
    raise SystemExit("사용 가능한 iPhone Simulator가 없습니다.")
print(selected["udid"])
')"
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

if ! xcrun simctl list devices booted -j | DEVICE_ID="$device_id" python3 -c '
import json, os, sys

device_id = os.environ["DEVICE_ID"]
booted = any(
    device["udid"] == device_id
    for entries in json.load(sys.stdin)["devices"].values()
    for device in entries
)
raise SystemExit(0 if booted else 1)
'; then
  xcrun simctl boot "$device_id"
  started_device=true
fi

xcrun simctl bootstatus "$device_id" -b
original_content_size="$(xcrun simctl ui "$device_id" content_size)"
derived_data="${TMPDIR:-/tmp}/ChaekchaekDerivedData"

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
