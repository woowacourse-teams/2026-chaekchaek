# 모바일 원격 설정과 업데이트 정책

## 목적과 적용 경계

이슈 [#581](https://github.com/woowacourse-teams/2026-chaekchaek/issues/581). Android와 iOS에 Firebase Remote Config를 연결하고 지원하지 않는 버전에는 업데이트 안내로 앱 사용을 막는다. 정책을 해석하는 코드가 포함된 버전부터 동작한다. 이미 배포된 기존 앱에 이 코드를 소급해서 넣을 수는 없다.

서버 인증, 권한, API 호환성은 앱의 다이얼로그로 보장하지 않는다. 강제 업데이트는 UI 차단 정책이다. 앱의 스토어 업데이트와 프로세스 종료에서는 이미 존재하는 영구 저장 정책이 적용된다. 업데이트 안내 표시 자체는 기존 내비게이션을 제거하지 않아 현재 입력과 로그인 상태를 유지한다.

## 원격 값과 고정값

단일 JSON 매개변수 `mobile_policy_v1`을 사용한다. 최소와 최신 버전이 따로 반영되는 것을 막기 위해 함께 검증한다.

| 항목 | 기본값 | 의미 |
| --- | --- | --- |
| schemaVersion | 1 | 지원하는 정책 형식 |
| androidMinimumVersionCode | 0 | 이 코드보다 낮으면 강제 업데이트 |
| androidLatestVersionCode | 0 | 최소 이상이고 이 코드보다 낮으면 권장 업데이트 |
| iosMinimumVersion | 0.0.0 | 이 버전보다 낮으면 강제 업데이트 |
| iosLatestVersion | 0.0.0 | 최소 이상이고 이 버전보다 낮으면 권장 업데이트 |
| verificationMessage | 빈 문자열 | 개발자 도구에서 실제 수신을 확인할 문구. UTF-16 200자 이내 |

현재 차단할 구버전이 특정되지 않아 기본 경계는 0으로 비활성화한다. 최소 버전은 보안, 재현되는 치명적 크래시, 호환 API 제거로 정상 이용할 수 없는 경우에만 올린다. 일반적인 개선은 최신 버전만 올린다. 최소 지원 버전은 임의로 최근 N개 버전을 뜻하지 않는다.

패키지 ID와 App Store ID, 스토어 주소, 수신 주기와 검증 규칙, UI 문구는 앱에 고정한다. 원격 주소 변조로 다른 사이트에 보내지 않는다. 비밀 키, 토큰, 사용자 개인정보는 원격 설정에 넣지 않는다. 클라이언트가 받은 값은 사용자가 읽을 수 있기 때문이다. 실제 기능을 끄는 스위치는 아직 대상 기능이 없으므로 추가하지 않았다.

Android는 versionCode 숫자로, iOS는 2~3자리 숫자 버전으로 비교한다. `1.10.0`은 `1.9.0`보다 크다. 음수, 잘못된 버전, 최신 < 최소, 미지원 schema, 4096자 초과 payload는 거절한다. 알 수 없는 필드는 무시해 같은 스키마의 필드 확장을 허용한다.

## 수신, 캐시와 적용 시점

- 앱은 첫 네트워크 요청을 기다리지 않고 기본값 또는 마지막 검증된 정책으로 시작한다.
- 시작과 foreground 복귀에서 수신을 요청한다. SDK 최소 수신 간격은 1시간, 개별 네트워크 수신 제한 시간은 10초다. 반복 복귀는 SDK 캐시를 사용한다.
- foreground 동안 실시간 Remote Config 리스너를 등록한다. 변경 통지를 받은 SDK는 일반 최소 수신 간격과 별개로 새 값을 받아 온다. 백그라운드에서는 리스너를 제거한다.
- 검증에 성공한 값은 즉시 적용한다. 일반 화면의 내용을 갑자기 바꾸는 설정은 현재 없다. 긴급 차단은 현재 작업 화면 위에 안내를 올리는 방식으로 적용한다.
- 디스크에는 검증된 JSON과 실제 SDK 수신 시각만 저장한다. 실패 또는 잘못된 정책은 기본값/마지막 정상값을 유지한다. 예전 응답이 새 수신 시각보다 오래됐으면 무시한다.
- 일반 갱신만 성공하는 환경에서는 마지막 성공 수신 후 1시간이 지난 다음 시작/복귀에 반영된다. 앱이 계속 백그라운드거나 오프라인이면 시간 상한을 보장하지 않는다. 실시간 연결 중에는 SDK 변경 통지와 수신이 끝난 뒤 반영되며 고정된 초 단위 SLA는 없다.
- 실시간 연결 실패, 서버 장애와 첫 실행 오프라인은 사용자 오류 팝업을 띄우지 않고 개발자 도구의 수신 결과에 기록한다. 마지막 정상 정책이 강제 업데이트였다면 오프라인에서도 차단을 유지한다.
- 권장 안내의 `나중에`는 해당 정책에 한해 현재 프로세스 동안 유지된다. 새 정책이나 다음 프로세스에서는 다시 안내할 수 있다. 강제 정책은 무시할 수 없다.

## 업데이트 왕복

| 상황 | Android | iOS |
| --- | --- | --- |
| 최소 미달 | `업데이트` 하나, 외부 탭/뒤로가기 닫기 불가. 가능한 경우 Immediate | 같은 공통 안내 후 App Store 이동 |
| 최소 이상, 최신 미달 | `나중에` 또는 `업데이트`. 가능한 경우 Flexible | 같은 공통 안내 후 App Store 이동 |
| 최신 이상 | 안내 없음 | 안내 없음 |
| Play 사용 불가/지원 안 함 | 고정 Play Store 주소로 이동. 실패하면 공통 오류 안내 | 해당 없음 |
| Flexible 다운로드 완료 | 사용자 확인 후 `다시 시작`으로 completeUpdate. 다음 복귀에서도 완료 상태 확인 | 해당 없음 |

Immediate 취소 시 강제 안내가 다시 남는다. Play에서 제공하는 코드가 원격 최소 코드보다 낮으면 Immediate를 시작하지 않고 스토어로 보낸다. 이때도 앱 차단은 유지되므로 잘못된 최소 버전을 발행하면 사용자가 갇힐 수 있다. **스토어에서 해당 대상 사용자가 실제로 업데이트를 설치할 수 있음을 먼저 확인한다.** 단계적 배포, 국가/트랙 차이를 포함한다. 앱의 최신 버전 값만 올린다고 스토어에 새 바이너리가 생기지는 않는다.

## 개발자 도구와 테스트

Android 기존 개발자 화면과 iOS Debug의 홈 `개발`에서 현재 정책, 출처, 마지막 수신 시각(UTC), 결과, 검증 문구를 확인한다. 강제 수신, 강제/권장/최신 임시 판정과 원복을 제공한다. 임시 판정은 디스크와 원격 값을 바꾸지 않으며 프로세스 종료 시 사라진다. Release에는 해당 진입점을 제공하지 않는다.

Android Debug에서는 FakeAppUpdateManager를 선택해 앱으로 돌아간 뒤 `업데이트`를 누른다. 개발자 도구에서 수락/취소/다운로드 완료를 전이시키고 실제 Play 매니저로 복귀할 수 있다. Fake는 실제 Play UI와 설치를 대신하지 않는다. 실제 In-App Updates는 Play가 설치한 앱과 사용자의 사용 권한이 필요하며 내부 앱 공유/테스트 트랙에서 같은 applicationId와 서명, 높은 versionCode를 사용해 확인한다.

iOS UI 테스트 launch arguments: `-uiTestingRequiredUpdate`, `-uiTestingRecommendedUpdate`. Release에서는 처리하지 않는다.

## Figma 원본

- [공통 UpdateDialog](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1524-6124): Required, Recommended, Downloaded
- [Controls 연결 인스턴스](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1524-6125)
- [개발자 도구](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1524-6867)

기존 오류 안내의 스타일, Gowun Dodum, 공통 버튼과 변수를 재사용했다. 파일 내 메인 컴포넌트와 연결 인스턴스를 등록하고 캡처를 확인했다. 팀 라이브러리 게시와 앱 기기 검증은 별개다.

## 운영과 하위 호환

1. API를 바꿀 때 설치된 구버전의 파싱 및 요청을 먼저 확인한다. 추가 필드와 선택적 필드는 기존 앱이 수용하는지 확인하며, 필수 필드 제거는 기존 앱의 코드가 이미 설치되어 있다는 사실을 고려한다.
2. 하위 호환 API는 구버전 사용 현황과 새 버전의 실제 배포 가능 여부를 확인할 때까지 유지한다. 현재 버전별 사용자 수 집계나 일괄적인 종료 날짜는 확정되지 않았다.
3. 호환성을 제거해야 하면 근거, 대상 최소 버전과 스토어 설치 근거를 기록하고, 먼저 원격 정책으로 이동을 유도한다. 업데이트 기능이 없는 오래된 앱은 별도 호환 API가 필요하다.
4. 배포 전 현재 템플릿을 다시 받아 기존 매개변수/조건을 보존하고 변경 diff를 검토한다. 초안 파일로 전체 템플릿을 무조건 대체하지 않는다.
5. 잘못된 차단의 복구는 해당 최소 경계를 내리는 새 정책 발행이다. 오프라인 사용자는 새 정책을 받기 전까지 기존 차단을 유지한다.

`android/config/remote-config/template.draft.json`은 검토용 원본이다. 사용자 승인 후 같은 정책을 2026-10-23 16:30:50 KST에 `chaekchaek-d2b85` Remote Config 버전 1로 게시하고 다시 조회했다. 최소/최신 경계는 모두 0이며 `verificationMessage`는 `원격 설정 연결 확인`이다. 사용자를 차단하지 않는 수신 검증 정책이다. 현재 접근 확인된 Firebase 프로젝트는 `chaekchaek-d2b85`다. 원격 검증기의 Android Release 설정도 `chaekchaek-d2b85`임을 확인했다. Android Debug는 별도 `chaekchaek-debug`를 사용하며 현재 계정의 접근은 확인되지 않았다. 두 프로젝트를 섞거나 SDK 설정을 자동으로 바꾸지 않는다.

Firebase는 기존 서비스와 원격 업데이트 정책에 사용하고 PostHog는 사용자 이벤트 분석에 사용한다. PostHog로 통합하는 전환 비용과 러닝커브를 고려해 현재는 둘을 유지한다. 이번 통합에서 PostHog 초기화와 이벤트 전달, Firebase Remote Config 연결을 함께 보존했다.

실제 Firebase 수신 UI 테스트는 게시 이후 별도로 실행한다. Xcode 테스트 프로세스에 `TEST_RUNNER_VERIFY_LIVE_REMOTE_POLICY=1`을 전달하고 `RemotePolicyUITests/testPublishedPolicyIsReceivedWithoutStoreRelease`를 실행한다. 일반 테스트에서는 외부 서비스 의존 검증을 건너뛴다. 테스트는 개발자 도구에서 강제 수신 후 원격 출처, 검증 문구와 마지막 수신 시각을 확인한다.

## 검증 상태

최신 `an-develop`의 PostHog 병합 커밋 `6a477fbb`를 반영했다. 이번 검증의 앱/테스트 코드 커밋은 `19873719`다. 이후 이 문서만 갱신하며 앱 소스는 바꾸지 않는다.

| 항목 | 결과와 근거 |
| --- | --- |
| Firebase 게시 | `chaekchaek-d2b85`, 버전 1, 2026-10-23 16:30:50 KST. 게시 후 다시 조회해 `mobile_policy_v1`과 차단 없는 경계를 확인했다. |
| iOS 실제 수신 | iPhone 17 Pro, iOS 26.2에서 실제 서비스 수신 테스트 1개 통과, 실패/건너뜀 0. 개발자 도구의 출처 `원격`, 수신 결과 `수신 성공`, 수신 시각 `2026-10-23T07:46:02.935Z`, 검증 문구 `원격 설정 연결 확인`을 캡처에서 확인했다. 스토어 배포 없이 게시한 값이 앱에 적용됐다. |
| iOS 공통 정책 | `iosSimulatorArm64Test`의 MobilePolicyTest 9개 통과, 실패/오류/건너뜀 0. 기본값, 오프라인, 캐시 복원, 잘못된 정책, 숫자 버전 경계, 임시 설정과 응답 순서를 검증한다. |
| iOS 앱과 UI | 통합 앱 빌드 성공. 강제 차단/닫기 불가와 권장 안내 미루기/홈 보존 UI 2개 통과. 네이티브 단위 테스트 9개도 통과했다. |
| Android 통합 빌드와 테스트 | 플리파 원격 접수 전에 SSH 연결 시간 초과를 확인해 Jev 로컬 대체 경로로 전환했다. 원격 제출은 하지 않았다. 같은 코드의 Release APK, Debug APK와 테스트 APK 빌드, `:app:lintDebug`, `:shared:testAndroidHostTest` 408개 통과, 실패/오류/건너뜀 0. AAB는 이번에 검증하지 않았다. |
| Android 설치와 UI | Pixel_6a_API_33_2, `emulator-5580`에서 같은 서명의 기존 `1.3.1 (10)`을 확인한 뒤 `install -r`로 갱신하고 MainActivity 실행을 확인했다. UpdatePolicyTest 2개 통과: 강제 안내 뒤로가기 차단과 입력 보존, 권장 안내 미루기와 상태 보존. |
| Android Jev | 고정 실행기 0.2.0, 소스 `a43a0e91` 확인. Portal 0.7.27의 패키지/서명 검사를 통과했으나 설치가 `INSTALL_FAILED_INSUFFICIENT_STORAGE`로 실패해 시나리오는 실행하지 못했다. 기존 데이터 삭제나 다른 AVD 우회를 하지 않았다. 자신이 부팅한 기기 종료와 임대 반환을 완료했다. |

검증 시작 시 문서 수정과 Xcode가 자동으로 넣은 테스트 선택 속성이 추적 변경으로 기록됐지만 앱/테스트 소스는 `19873719`와 동일하다. 자동 scheme 속성은 검증 후 원복했다.

첫 iOS 실제 수신 실행은 `CODE_SIGNING_ALLOWED=NO`로 빌드해 Firebase Installations의 키체인 접근 오류 `SecItemCopyMatching (-34018)`가 발생했다. 앱은 기본값과 차단 없음 상태를 유지했다. 시뮬레이터 검증에서는 `CODE_SIGNING_ALLOWED=YES CODE_SIGN_IDENTITY=- CODE_SIGN_STYLE=Manual`로 빌드한 뒤 실제 수신 테스트를 다시 실행해 통과했다. 운영 계정이나 정책을 바꿔 우회하지 않았다.

결과 묶음:

- iOS 공통 테스트 로그와 UI 2개/네이티브 9개 결과: `/tmp/chaekchaek-581-19873719-ios/RemotePolicy.xcresult`, 같은 폴더의 `common-tests.log`.
- iOS 실제 수신 성공: `/tmp/chaekchaek-581-19873719-ios-signed/RemotePolicy.xcresult`.
- Android 빌드, 설치 검사와 UI 결과: `/tmp/chaekchaek-581-19873719-android/`의 `build.log`, `app-preflight.json`, `instrumentation.log`.
- 실제 수신 캡처: `/tmp/chaekchaek-581-ios-signed-attachments/EFA9B84E-41FB-42CC-8A27-BCE72C8A6470.png`.

### Android 물리기기 실제 정책 왕복 (2026-10-23)

소스 `bc1f5cef`의 Release 빌드를 SM-S931N (`R3CY709C7BD`)에 설치했다. 기존 앱과 같은 `1.3.1 (10)`이며, 운영 Firebase `chaekchaek-d2b85` 연결을 유지했다. 기존 앱이 로컬 Debug 키로 서명되어 있어 검증용 Release 빌드만 같은 키로 서명했다. 설치 전 패키지, 버전과 인증서 SHA-256 일치를 확인하고 `adb install -r`로 설치했다. 앱 삭제나 데이터 초기화는 하지 않았다. 이 APK는 스토어 배포 서명의 검증 근거가 아니다.

- 사용자 승인 후 17:19:56 KST에 Remote Config 버전 2를 게시했다. Android 최소/최신은 11, iOS 최소/최신은 1.3.2로 설정하고 나머지 값은 보존했다.
- 게시 후 서버 재조회 완료부터 약 2.5초 뒤 Android 화면 구조에서 `업데이트가 필요해요`, 안내 본문과 `업데이트` 버튼을 확인했다. `나중에` 버튼은 없었다. 앱 재설치나 재시작 없이 기존 홈 위에 원격 정책이 반영됐다. 이 측정은 한 번의 관측이며 반영 시간 보장값이 아니다.
- 사용자가 직접 업데이트 버튼을 눌러 캡처에는 Play 전환 중 화면이 담겼다. 자동 뒤로가기 검사와 수동 조작이 겹쳐 이번 실행만으로 뒤로가기 차단을 독립 검증했다고 판정하지 않는다. 해당 동작의 기존 Android instrumentation 검증은 위 표와 같다.
- 17:20:17 KST에 버전 3으로 기존 정책을 복원했다. 게시 간격은 21초다. 서버를 다시 조회해 Android 최소/최신 0, iOS 최소/최신 0.0.0과 기존 검증 문구를 확인했다.
- 책췍을 앞으로 가져온 뒤 강제 팝업이 사라지고 홈이 보이는 것을 화면 구조와 캡처로 확인했다. 앱 데이터 삭제 없이 차단 해제까지 반영됐다.
- 이번에는 Android 물리기기의 실제 Firebase 수신과 복원을 확인했다. iOS 기기의 동일 운영 정책 왕복과 Play 실제 업데이트 설치는 확인하지 않았다.

근거는 `/tmp/chaekchaek-581-physical/`의 `preflight.json`, `physical-release-build.log`, `required.xml`, `required-result.json`, `restored.xml`, `restored.png`, `restored-result.json`과 `policy/`의 게시/복원 템플릿 및 `versions.log`다. `required.png`는 수동 터치로 인한 Play 전환 캡처이므로 팝업 전체의 시각 검증 자료로 사용하지 않는다. 임시 경로의 자료는 기기 검증 시점의 로컬 근거이며 저장소에 영구 보관한 첨부는 아니다.

남은 확인:

- Android Debug의 `chaekchaek-debug` 접근과 실제 원격 수신은 미검증이다. 운영 프로젝트 게시와 iOS 수신을 Debug 프로젝트 수신의 증거로 사용하지 않는다.
- FakeAppUpdateManager의 전체 왕복은 기기에서 미검증이다.
- Play 내부 앱 공유/테스트 트랙의 낮은 버전에서 Flexible/Immediate 설치를 끝까지 확인하지 않았다. 기존 앱을 adb로 덮어쓴 결과를 In-App Updates의 증거로 사용하지 않는다.
- iOS App Store 이동과 실제 업데이트 후 복귀는 미검증이다. 이미 배포된 정책 미지원 앱을 이 구현으로 소급 차단할 수 없다.

실제 Firebase 수신과 구버전 스토어 업데이트 완료 조건은 별도다. 현재는 전체 요구사항 완료나 이슈 종료로 판정하지 않는다.

## 공식 근거

- [Remote Config 로딩 전략](https://firebase.google.com/docs/remote-config/loading)
- [Android Remote Config](https://firebase.google.com/docs/remote-config/android/get-started)
- [iOS Remote Config](https://firebase.google.com/docs/remote-config/ios/get-started)
- [Play In-App Updates 구현](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java)
- [Play In-App Updates 테스트](https://developer.android.com/guide/playcore/in-app-updates/test)
