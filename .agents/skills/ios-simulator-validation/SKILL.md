---
name: ios-simulator-validation
description: 책췍 KMP iOS 앱을 iOS Simulator에서 빌드, 테스트, UI 조작, 스크린샷과 Apple HIG 기준으로 검증한다. iOS 화면 검증, Dynamic Type, 접근성 감사, Simulator 확인, Apple 로그인 UI 확인 요청에 사용하며 Android Emulator 검증에는 사용하지 않는다.
---

# iOS Simulator 검증

대상은 `android/iosApp/iosApp.xcodeproj`의 `Chaekchaek` scheme이다.

## 플랫폼별 검증 실행권

iOS 빌드·Simulator 부팅·테스트·아카이브 전에 `~/.agents/skills/mobile-validation-queue/SKILL.md`를 읽고 공용 실행기로 실행한다. 같은 Mac의 Android 빌드·검증과 동시에 무거운 작업을 시작하지 않는다. 명시적 UDID를 사용하고 다른 세션이 실행 중인 Simulator를 인수하거나 종료하지 않는다. 자신이 시작한 기기 정리까지 슬롯을 유지한다. `test-ios-hig.sh`는 실행기를 자체 호출하므로 중복 대기 없이 같은 슬롯을 사용한다.

Android 검증은 `~/.agents/skills/plipa-remote-validation/SKILL.md`로 제출한다. 원격 작업 대기는 Mac 슬롯을 점유하지 않으므로 같은 SHA의 iOS 검증과 병행할 수 있다. 설정 누락·장애·미지원 범위에서 자동으로 로컬 Android를 실행하지 않는다. 사용자에게 명시적으로 허용된 로컬 Android도 공용 대기열을 사용한다.

## 원칙

- 동시에 부팅된 iOS Simulator는 최대 2대로 제한한다. 현재 대기열 작업이 직접 시작한 적합한 기기가 있으면 재사용한다. 다른 세션 또는 소유자가 불명확한 실행 기기는 인수하지 않는다.
- Orca나 CLI 환경에서는 Computer Use 연결을 전제로 하지 않는다. 앱 UI 조작은 XCUITest, 기기와 앱 관리는 `xcrun simctl`을 사용한다.
- 저장소를 변경하지 않는 검증을 우선한다. 영구 UI 테스트 추가가 필요하면 기존 작업 브랜치의 범위와 사용자 요청에 포함되는지 먼저 확인한다.
- 앱 시작만 확인하고 화면 검증이 끝났다고 판단하지 않는다. 요청된 사용자 흐름을 재현하고 최종 상태를 스크린샷으로 확인한다.
- 검증을 위해 부팅한 Simulator만 종료한다. 기존에 부팅되어 있던 Simulator는 유지한다.
- `android/shared/src/commonMain/**` 중 iOS에서 렌더링되는 UI, `android/shared/src/iosMain/**`, `android/iosApp/**`를 iOS 검증 대상으로 본다.
- Apple 또는 Google 등 공급업체 소유 네이티브 로그인 컨트롤의 내부 스타일은 변경하지 않는다. 앱에서는 배치, 외부 여백, 가시성과 접근 가능한 hit target만 검증한다.

## 현재 검증 범위

일반 구현과 배포에서는 기본 글자 크기의 가독성, 버튼의 실제 조작, 입력, 스크롤과
요청한 화면 흐름을 필수로 확인한다. AX5, 엄격한 44x44pt 기준과 대비 수치 등
접근성 감사만의 실패는 비차단 항목으로 기록한다. 기본 크기에서 핵심 동작을
막는 가림과 잘림은 기능 결함이므로 이 예외를 적용하지 않는다.
기존 접근성 테스트와 라벨, 역할은 유지한다. 감사 실패로 기능 검증이 중단되면
남은 기능은 별도로 확인하고, 감사 실패를 테스트 통과로 바꾸어 보고하지 않는다.
전체 HIG 및 접근성 검증은 명시적인 요청이나 해당 개선 작업에서 아래 확장 절차를 적용한다.

## 확장 HIG 검증 기준

공식 기준은 2026-08-29에 확인했다.

- [Apple Human Interface Guidelines](https://developer.apple.com/kr/design/human-interface-guidelines)
- [Typography](https://developer.apple.com/kr/design/human-interface-guidelines/typography): iOS 기본 본문 17pt 참고, 사용자 표시 텍스트 최소 11pt
- [Accessibility](https://developer.apple.com/kr/design/human-interface-guidelines/accessibility): Dynamic Type로 확대 가능한 레이아웃
- [UI Design Tips](https://developer.apple.com/design/tips/): 최소 44x44pt hit target
- [Performing accessibility audits](https://developer.apple.com/documentation/accessibility/performing-accessibility-audits-for-your-app): XCUITest `performAccessibilityAudit()`

대표 화면의 앱 소유 텍스트가 디자인 시스템의 11개 의미 역할에 연결됐는지 확인하고, 기본 Dynamic Type 크기와 AX5에서 각각 검증한다. AX5는 Simulator의 `accessibility-extra-extra-extra-large`이다. 각 상태에서 텍스트 잘림과 겹침, 의도하지 않은 가로 스크롤, 핵심 동작의 손실, 사용자 표시 텍스트 11pt 미만 여부, 직접 누르는 컨트롤의 44x44pt hit target을 확인한다.

## 검증 절차

1. `git status --untracked-files=no`로 대상 worktree의 추적 변경을 확인한다.
   사용자 전용 `xcuserdata`의 같은 이름 scheme이 공유 scheme을 가려 `test action` 또는 `test-without-building action` 오류가 나면 사용자 scheme을 수정하거나 삭제하지 않는다. 같은 커밋의 임시 detached worktree에 현재 작업의 버전 및 소스 변경만 동일하게 적용하고 공유 scheme으로 검증한다. 검증한 소스의 일치를 확인하고 임시 worktree만 정리한다.
2. `xcrun simctl list devices booted`로 현재 기기 수와 UDID, runtime을 기록한다.
3. 적합한 기기 한 대를 선택하고 `xcrun simctl ui <UDID> content_size`로 원래 Dynamic Type 크기를 기록한다.
4. 전체 HIG 및 접근성 검증을 요청받았을 때는 저장소 루트에서 다음 스크립트로 실행한다. 선택할 Simulator UDID를
   인자로 전달할 수 있으며, 생략하면 이미 부팅된 iPhone을 우선 사용한다.

   ```sh
   android/scripts/test-ios-hig.sh [UDID]
   ```

   스크립트는 `build-for-testing`을 한 번 실행한 뒤 기본 크기와 AX5에서
   `test-without-building`을 실행하고, 완료 시 원래 Dynamic Type 크기와 기기 상태를
   복원한다.

5. UI 조작이 필요하면 기존 XCUITest target을 우선 사용한다. 접근성 식별자나 표시 문자열로 요소를 찾고 요청 흐름을 재현한다. 고정 좌표 탭은 최후 수단으로만 사용한다. 전체 접근성 검증 범위에서는 도달한 화면에서 `try app.performAccessibilityAudit()`를 실행한다. 일반 기능 검증에 새 감사를 의무적으로 추가하지 않는다.
6. 일부 테스트만 수동 실행할 때는 `xcrun simctl ui <UDID> content_size large`로 기본 크기를 설정하고 앱을 다시 실행해 검증한다.
7. AX5 검증이 요청 범위에 포함되고 일부 테스트만 수동 실행할 때는 `xcrun simctl ui <UDID> content_size accessibility-extra-extra-extra-large`로 AX5를 설정하고 앱을 다시 실행해 같은 흐름을 검증한다.
8. 빌드된 앱을 직접 실행해야 하면 `xcrun simctl install`과 `xcrun simctl launch`를 사용한다.
9. 각 Dynamic Type 상태에서 `xcrun simctl io <UDID> screenshot <절대경로>`로 최종 화면을 저장하고 직접 확인한다.
10. 검증이 끝나면 3단계에서 기록한 Dynamic Type 크기로 복원한다.
11. 결과에 기기, runtime, 실행한 테스트, 기본 크기와 AX5 결과, 접근성 감사 결과, 성공과 실패, 스크린샷 경로, 자동화로 확인하지 못한 항목을 구분해 보고한다.

테스트 실패 시 첫 번째 원인을 확인하고 기능 실패와 접근성 감사 실패를 구분한다. 기능 실패는 해결하거나 명확히 보고하고, 접근성 감사만의 실패는 기록한 뒤 필요한 화면 및 기능 검증을 계속한다.
Simulator 상태 설정, 대상 화면 도달, 스크린샷 또는 `performAccessibilityAudit()` 중 하나라도 실패하면 HIG 준수로 판정하지 않는다. 실행 실패와 감사에서 확인된 결함을 구분하고 실패 원인과 재현 명령을 함께 보고한다. 이 판정은 일반 구현과 배포의 차단 조건과 구분한다.

## 릴리즈 검증과 부분 가림 회귀

- 기본 크기의 성공으로 AX5 성공을 대신하지 않는다. 각 실행의 통과, 실패와 건너뜀 개수를 따로 집계하고 fixture 의존 건너뜀은 검증 완료로 세지 않는다. production 환경에서는 답글 왕복과 감상 저장 복구 fixture 테스트가 건너뛰어질 수 있으므로 실제 결과와 사유를 확인한다.
- 배포 작업에서는 기본 크기의 필수 기능 검증 결과를 확인하고, AX5 전체 실행을 업로드의 필수 선행 조건으로 삼지 않는다. 접근성만의 실패나 보류 때문에 승인된 배포를 중단하거나 같은 사유로 재승인을 요구하지 않는다. Simulator의 Debug 성공과 Release Archive 성공도 별도로 확인한다. 테스트 이후 소스가 바뀌었으면 이전 바이너리로 `test-without-building`만 재실행하지 말고 변경 소스를 먼저 빌드한 뒤 관련 시나리오를 다시 실행한다.
- 책 상세 자료 패널을 열고 배경 목록을 스크롤하는 회귀에서는 패널 경계의 필터, 뒤로 가기뿐 아니라 하단 감상 작성 영역에 일부 가려지는 좋아요 버튼도 확인한다. 코드의 원래 크기만 보지 말고 접근성 트리에 노출된 실제 frame과 hit target을 감사한다. 실패 요소는 테스트 이름이 아니라 감사 로그의 label, frame 및 캡처로 특정한다.
- 확인된 회귀: `1.3.1 (10)`, iPhone 17 Pro, iOS 26.2의 AX5에서 `QuestionAPreviewUITests/testPartiallyCoveredFiltersAndBackReturnAfterReferenceCloses`가 `감상 좋아요`의 높이 약 14.3pt와 `Hit area is too small`로 실패했다. 기본 크기는 통과했고 최종 소스의 AX5 단독 실행에서도 재현됐다. 2026-10-21 사용자 결정에 따라 현재는 배포 비차단 접근성 결함으로 보류한다. 테스트나 결함 기록을 삭제하거나 통과로 바꾸지 않으며, 접근성 개선 요청 때 재현한다. 기본 크기에서 실제 조작이 막히는 문제가 확인되면 기능 결함으로 다시 분류한다.
- 재현은 기본 절차의 Simulator 선택과 상태 복원을 유지한 채 AX5로 설정하고, 같은 프로젝트 및 공유 scheme의 `xcodebuild test`에 `-only-testing:ChaekchaekUITests/QuestionAPreviewUITests/testPartiallyCoveredFiltersAndBackReturnAfterReferenceCloses`를 지정한다. 실패 시 xcresult의 감사 로그, 접근성 트리와 캡처를 보존한다. `xcrun xcresulttool export attachments --help`로 현재 옵션을 확인해 테스트 첨부 캡처를 추출할 수 있다. 추출한 캡처와 새로 촬영한 캡처의 출처를 구분한다.

근거와 배포 상태는 [1.3.1 검증 기록](../../../docs/android-release-management.md#131-10)을 참고한다. Simulator 결과는 TestFlight 처리 완료나 실기기 검증을 대신하지 않는다.

검증 진입점과 기기 보호를 변경하면 `python3 android/scripts/test-mobile-queue-integration.py`로 가짜 Apple 도구 기반 회귀 검사를 실행한다. 이 검사는 실제 Simulator 실행 증거와 구분한다.
