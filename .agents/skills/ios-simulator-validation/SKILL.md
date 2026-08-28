---
name: ios-simulator-validation
description: 첵췍 KMP iOS 앱을 iOS Simulator에서 빌드, 테스트, UI 조작, 스크린샷으로 검증한다. iOS 화면 검증, Simulator 확인, Apple 로그인 UI 확인 요청에 사용하며 Android Emulator 검증에는 사용하지 않는다.
---

# iOS Simulator 검증

대상은 `android/iosApp/iosApp.xcodeproj`의 `Chaekchaek` scheme이다.

## 원칙

- 동시에 부팅된 iOS Simulator는 최대 2대로 제한한다. 적합한 기기가 이미 있으면 새 기기를 부팅하지 않는다.
- Orca나 CLI 환경에서는 Computer Use 연결을 전제로 하지 않는다. 앱 UI 조작은 XCUITest, 기기와 앱 관리는 `xcrun simctl`을 사용한다.
- 저장소를 변경하지 않는 검증을 우선한다. 영구 UI 테스트 추가가 필요하면 기존 작업 브랜치의 범위와 사용자 요청에 포함되는지 먼저 확인한다.
- 앱 시작만 확인하고 화면 검증이 끝났다고 판단하지 않는다. 요청된 사용자 흐름을 재현하고 최종 상태를 스크린샷으로 확인한다.
- 검증을 위해 부팅한 Simulator만 종료한다. 기존에 부팅되어 있던 Simulator는 유지한다.

## 검증 절차

1. `git status --untracked-files=no`로 대상 worktree의 추적 변경을 확인한다.
2. `xcrun simctl list devices booted`로 현재 기기 수와 UDID, runtime을 기록한다.
3. 적합한 기기 한 대를 선택하고 다음 형식으로 테스트한다.

   ```sh
   xcodebuild test \
     -project android/iosApp/iosApp.xcodeproj \
     -scheme Chaekchaek \
     -destination 'platform=iOS Simulator,id=<UDID>' \
     CODE_SIGNING_ALLOWED=NO
   ```

4. UI 조작이 필요하면 기존 XCUITest target을 우선 사용한다. 접근성 식별자나 표시 문자열로 요소를 찾고 요청 흐름을 재현한다. 고정 좌표 탭은 최후 수단으로만 사용한다.
5. 빌드된 앱을 `xcrun simctl install`과 `xcrun simctl launch`로 실행한다.
6. `xcrun simctl io <UDID> screenshot <절대경로>`로 최종 화면을 저장하고 직접 확인한다.
7. 결과에 기기, runtime, 실행한 테스트, 성공과 실패, 스크린샷 경로, 자동화로 확인하지 못한 항목을 구분해 보고한다.

테스트 실패 시 화면 검증으로 넘어가지 말고 첫 번째 원인을 해결하거나 명확히 보고한다.
