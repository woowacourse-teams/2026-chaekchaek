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

`android/config/remote-config/template.draft.json`은 배포되지 않은 검토용 템플릿이다. 최소/최신 경계는 0이고 검증 문구만 있다. 현재 접근 확인된 Firebase 프로젝트는 `chaekchaek-d2b85`다. 원격 검증기의 Android Release 설정도 `chaekchaek-d2b85`임을 확인했다. Android Debug는 별도 `chaekchaek-debug`를 사용하며 현재 계정의 접근은 확인되지 않았다. 두 프로젝트를 섞거나 SDK 설정을 자동으로 바꾸지 않는다.

## 검증 상태

2026-10-22~23 검증 코드 커밋: `45cee6e70edac0f8106a936f5946c4c06aaceede`. 이후 검증 기록만 갱신하는 커밋은 앱 소스를 바꾸지 않는다.

| 항목 | 결과와 근거 |
| --- | --- |
| Android 원격 실행 경로 | `an-develop`과 `581-remote-config-update`를 허용했다. 실행 중인 검증기의 설정을 다시 읽게 한 뒤 해당 브랜치의 정확한 SHA로 작업이 수락됐다. |
| Android Release, 단위 테스트 | `android-smoke` 작업 `d8ef89ffe3b74a4a962b124e3497cd6b` 성공. Release APK 빌드, 단위 테스트 388개 통과, 실패/오류/건너뜀 0. 공통 정책 테스트 9개를 포함한다. |
| Android 설치와 실행 | 동일 서명의 기존 `1.1.1 (5)`을 확인하고 `1.3.1 (10)`을 덮어쓰기 설치했다. 프로세스와 MainActivity foreground를 확인했다. 실제 사용자 데이터 왕복과 전체 UI 시나리오의 증거는 아니다. |
| iOS 공통 정책 | `iosSimulatorArm64Test`에서 MobilePolicyTest 9개 통과, 실패/오류/건너뜀 0. 첫 실행, 오프라인, 캐시 복원, 잘못된 정책, 숫자 버전 경계, 임시 설정과 응답 순서를 검증한다. |
| iOS 앱과 UI | 최종 SHA 앱 빌드 성공. iOS 소스가 동일한 `e19a087f`에서는 iPhone 17 Pro, iOS 26.2로 강제/권장 UI 2개가 통과했고 캡처에서 문구와 버튼을 확인했다. 최종 SHA의 UI 실행은 테스트 도구의 Accessibility 초기화 시간 초과로 시작하지 못했다. 한 차례 재시도도 시뮬레이터 초기 데이터 이관 지연으로 중단했다. 최종 SHA UI 통과로 기록하지 않는다. |

Android 원격 결과 묶음 `android-45cee6e7.zip`, iOS `RemotePolicy-45cee6e7.xcresult`와 공통 테스트 로그를 검증 세션의 결과 디렉터리에 수집한다. 원격 검증 프로필은 전체 Lint, AAB, Android instrumentation을 실행하지 않는다. Release 빌드에 포함된 lintVitalRelease 통과를 전체 Lint 통과로 해석하지 않는다.

남은 확인:

- Firebase 초안은 사용자 검토 대기이며 게시하지 않았다. 실제 값 변경과 앱 수신은 아직 미검증이다.
- Android instrumentation의 팝업/입력 보존 테스트는 추가했지만 현재 원격 프로필이 지원하지 않아 실행하지 않았다. FakeAppUpdateManager의 전체 왕복도 기기에서 미검증이다.
- Play 내부 앱 공유/테스트 트랙의 낮은 버전에서 Flexible/Immediate 설치를 끝까지 확인하지 않았다. 기존 앱을 adb로 덮어쓴 결과를 In-App Updates의 증거로 사용하지 않는다.
- iOS App Store 이동과 실제 업데이트 후 복귀는 미검증이다. 이미 배포된 정책 미지원 앱을 이 구현으로 소급 차단할 수 없다.

초안이나 Fake 결과로 실서비스 수신과 구버전 스토어 업데이트 완료 조건을 대체하지 않는다.

## 공식 근거

- [Remote Config 로딩 전략](https://firebase.google.com/docs/remote-config/loading)
- [Android Remote Config](https://firebase.google.com/docs/remote-config/android/get-started)
- [iOS Remote Config](https://firebase.google.com/docs/remote-config/ios/get-started)
- [Play In-App Updates 구현](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java)
- [Play In-App Updates 테스트](https://developer.android.com/guide/playcore/in-app-updates/test)
