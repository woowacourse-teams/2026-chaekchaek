# 모바일 원격 설정과 업데이트 정책

문서 갱신: 2026-10-29. 구현 설명은 `an-develop`의 `c28bab7c` 기준이다. 실서비스 검증 기록은 아래에 적힌 2026-10-23 당시 결과이며, 이번 문서화에서는 운영 설정을 조회하거나 변경하지 않았다.

- [구현 구조](#구현-구조)
- [설정값과 버전 판정](#원격-값과-고정값)
- [Firebase에서 설정하기](#firebase에서-설정하기)
- [수신과 적용 시점](#수신-캐시와-적용-시점)
- [개발자 도구](#개발자-도구와-테스트)
- [요구사항별 달성 상태](#요구사항별-달성-상태)
- [실제 검증 기록](#검증-상태)

## 목적과 적용 경계

이슈 [#581](https://github.com/woowacourse-teams/2026-chaekchaek/issues/581). Android와 iOS에 Firebase Remote Config를 연결하고 지원하지 않는 버전에는 업데이트 안내로 앱 사용을 막는다. 정책을 해석하는 코드가 포함된 버전부터 동작한다. 이미 배포된 기존 앱에 이 코드를 소급해서 넣을 수는 없다.

서버 인증, 권한, API 호환성은 앱의 다이얼로그로 보장하지 않는다. 강제 업데이트는 UI 차단 정책이다. 앱의 스토어 업데이트와 프로세스 종료에서는 이미 존재하는 영구 저장 정책이 적용된다. 업데이트 안내 표시 자체는 기존 내비게이션을 제거하지 않아 현재 입력과 로그인 상태를 유지한다.

## 구현 구조

Firebase는 정책을 전달하고, 업데이트 필요 여부는 Kotlin 공통 코드가 판정한다. 플랫폼별 SDK와 스토어 연결은 어댑터로 분리했다. Android와 iOS가 같은 JSON을 해석하므로 버전 경계와 실패 시 동작을 공유한다.

```text
앱 시작 -> 기본값 또는 마지막 정상 캐시로 화면 표시
                    |
시작 / foreground 복귀 / 실시간 변경 통지
                    |
Firebase fetch -> activate -> JSON 검증 -> 정상 정책 저장
                    |
설치 버전과 최소·최신 비교 -> 공통 업데이트 팝업
                    |
Android: Play Immediate / Flexible 또는 스토어 이동
iOS: App Store 이동
```

| 담당 | 실제 코드 |
| --- | --- |
| JSON 스키마, 검증, 숫자 버전 비교 | [MobilePolicy.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/config/MobilePolicy.kt) |
| 마지막 정상값, 수신 시각, 상태와 임시 판정 | [MobilePolicyController.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/config/MobilePolicyController.kt) |
| Android Firebase 수신과 SharedPreferences 저장 | [AndroidRemotePolicy.kt](../android/app/src/main/java/com/chamsae/chaekchaek/AndroidRemotePolicy.kt) |
| Android Play 업데이트, 취소·복귀·다운로드 완료 | [AndroidAppUpdates.kt](../android/app/src/main/java/com/chamsae/chaekchaek/AndroidAppUpdates.kt) |
| iOS Firebase 수신, UserDefaults 저장, App Store 이동 | [ContentView.swift의 IOSRemotePolicy](../android/iosApp/Chaekchaek/ContentView.swift) |
| 두 플랫폼의 공통 팝업과 정책 개발자 도구 | [UpdatePolicyHost.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/common/UpdatePolicyHost.kt) |
| Android Fake 업데이트 조작 | [DeveloperToolsActivity.kt](../android/app/src/debug/java/com/chamsae/chaekchaek/DeveloperToolsActivity.kt) |
| 기본값·캐시·실패·버전 경계 회귀 테스트 | [MobilePolicyTest.kt](../android/shared/src/commonTest/kotlin/com/chaekchaek/app/config/MobilePolicyTest.kt) |

SDK가 값을 받았다는 사실만으로 사용하지 않는다. 원격 출처인 값을 공통 검증기로 검사한 뒤 저장한다. Android는 `mobile_policy` SharedPreferences의 `accepted`, iOS는 UserDefaults의 `accepted_mobile_policy`에 수신 시각과 JSON을 함께 저장한다. 앱이 재시작되면 저장된 정책도 검증하고 설치된 현재 버전으로 다시 판정한다.

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

최초 도입 시 차단할 구버전이 특정되지 않아 앱 내 기본 경계는 0으로 비활성화했다. 현재 서버의 운영값은 게시 전에 별도로 조회한다. 최소 버전은 보안, 재현되는 치명적 크래시, 호환 API 제거로 정상 이용할 수 없는 경우에만 올린다. 일반적인 개선은 최신 버전만 올린다. 최소 지원 버전은 임의로 최근 N개 버전을 뜻하지 않는다.

패키지 ID와 App Store ID, 스토어 주소, 수신 주기와 검증 규칙, UI 문구는 앱에 고정한다. 원격 주소 변조로 다른 사이트에 보내지 않는다. 비밀 키, 토큰, 사용자 개인정보는 원격 설정에 넣지 않는다. 클라이언트가 받은 값은 사용자가 읽을 수 있기 때문이다. 실제 기능을 끄는 스위치는 아직 대상 기능이 없으므로 추가하지 않았다.

Android는 versionCode 숫자로, iOS는 2~3자리 숫자 버전으로 비교한다. `1.10.0`은 `1.9.0`보다 크다. 음수, 잘못된 버전, 최신 < 최소, 미지원 schema, 4096자 초과 payload는 거절한다. 알 수 없는 필드는 무시해 같은 스키마의 필드 확장을 허용한다.

## Firebase에서 설정하기

### 1. 대상 프로젝트와 앱 확인

[Firebase Console](https://console.firebase.google.com/project/chaekchaek-d2b85/config)에서 책췍 프로젝트에 접근할 수 있는 계정으로 로그인한다. 기본 CLI 계정이 책췍 계정이라고 가정하지 않는다.

- 운영 수신 검증에 사용한 프로젝트: `chaekchaek-d2b85`.
- Android Debug는 별도 `chaekchaek-debug` 연결이다. 운영 프로젝트에서 값을 바꿔도 다른 프로젝트에 연결된 앱에는 전달되지 않는다.
- 실제 빌드에 포함되는 Android `google-services.json`, iOS `GoogleService-Info.plist`의 프로젝트 연결을 확인한다. 로컬 설정 파일 전체를 로그나 문서에 복사하지 않는다.
- Android 기준은 설치 앱의 `versionCode`이며 화면에 보이는 `versionName`이 아니다. iOS 기준은 `CFBundleShortVersionString`이며 빌드 번호가 아니다.

### 2. 매개변수 편집

Console의 **Remote Config > Parameters**에서 클라이언트 템플릿의 `mobile_policy_v1`을 연다. 처음 만드는 경우 이름을 정확히 입력하고 유형을 **JSON**으로 선택한다. 서버용 템플릿이 아니라 앱 SDK가 받는 클라이언트 템플릿을 사용한다. 메뉴 위치는 [공식 템플릿 관리 문서](https://firebase.google.com/docs/remote-config/templates/client)를 기준으로 찾는다.

다음은 **차단과 권장을 모두 비활성화하는 예시**다. 현재 서버 값의 사본이 아니다. Console의 매개변수 값에는 이 JSON 객체를 입력한다.

```json
{
  "schemaVersion": 1,
  "androidMinimumVersionCode": 0,
  "androidLatestVersionCode": 0,
  "iosMinimumVersion": "0.0.0",
  "iosLatestVersion": "0.0.0",
  "verificationMessage": "원격 설정 연결 확인"
}
```

[template.draft.json](../android/config/remote-config/template.draft.json)은 Firebase 전체 템플릿 형식이다. 여기서는 위 객체가 `parameters.mobile_policy_v1.defaultValue.value` 안의 JSON 문자열로 들어간다. Console 값 입력 칸에 전체 템플릿을 붙여 넣지 않는다. 이 초안으로 운영 템플릿 전체를 덮어쓰지도 않는다.

다음 표는 **설치 앱이 Android 코드 10, iOS 1.3.1이라고 가정한 설명용 값**이다. 실제 게시에는 현재 설치 버전과 스토어에서 설치 가능한 버전을 사용한다.

| 목적 | Android 최소 / 최신 | iOS 최소 / 최신 | 가정한 앱의 결과 |
| --- | --- | --- | --- |
| 안내 없음 | 0 / 0 | 0.0.0 / 0.0.0 | 정상 이용 |
| 새 버전 권장 | 0 / 11 | 0.0.0 / 1.3.2 | `나중에`, `업데이트` |
| 구버전 차단 | 11 / 11 | 1.3.2 / 1.3.2 | `업데이트`만 표시 |

판정은 `설치 버전 < 최소`이면 강제, 그 외 `설치 버전 < 최신`이면 권장이다. 최소와 같으면 지원 대상이며 최신과 같거나 높으면 권장 안내도 없다. 한 플랫폼만 변경할 때는 나머지 플랫폼 필드를 기존 값으로 유지한다.

`verificationMessage`만 바꾸면 차단 없이 원격 수신을 확인할 수 있다. 이 문구는 개발자 도구에 표시되며 일반 사용자 공지로 노출되지 않는다. 사용자 공지나 개별 기능 스위치는 현재 스키마에 없으므로 Console에 새 키만 추가해도 앱 동작은 바뀌지 않는다. 이를 지원하려면 공통 스키마의 기본값과 실제 기능 사용 지점에 연결하는 앱 변경이 먼저 필요하다.

### 3. 검토 후 게시

1. 현재 전체 템플릿과 버전 번호를 보관하고 변경 전 `mobile_policy_v1` 값을 기록한다. 기존 조건부 값이 있다면 테스트 기기에 어떤 값이 적용되는지도 확인한다.
2. 최소 버전을 올리는 이유와 대상 사용자가 실제 스토어에서 설치할 수 있는 새 버전을 확인한다. 일반 개선이면 최신 값만 올린다.
3. JSON의 최소·최신 관계, 변경 대상 플랫폼, 다른 매개변수와 조건이 보존되는지 diff를 검토한다. 앱은 누락 필드에 기본값을 사용하므로 운영 편집에서는 모든 필드를 유지한다.
4. Console에서 저장 후 **변경사항 게시**를 실행한다. 저장만 하고 게시하지 않으면 앱에는 새 값이 전달되지 않는다. 에이전트에게 게시를 맡길 때는 변경값과 복원 범위를 검토하고 게시를 승인한다.
5. 게시 후 현재 템플릿을 다시 조회하고 변경 이력의 버전·시각을 기록한다. 이 단계는 서버 반영 확인이다.
6. 같은 프로젝트에 연결된 앱을 foreground로 가져온다. Debug에서는 `설정 다시 받기`를 누르고 출처 `원격`, 새 검증 문구, 마지막 수신 시각과 판정을 확인한다. Release는 실제 팝업 표시 또는 해제를 확인한다.

CLI를 쓰는 경우에도 전체 템플릿을 먼저 받아 수정하고, `firebase.json`의 `remoteconfig.template`을 수정 파일로 연결한 뒤 `deploy --only remoteconfig`로 게시한다. 계정과 프로젝트를 명시하고, 게시 전 diff 검토와 게시 후 재조회를 동일하게 수행한다. 명령 형식은 [Firebase 공식 Remote Config 관리 안내](https://firebase.google.com/docs/remote-config/templates/client)를 따른다.

### 4. 일시 강제 차단 테스트와 복원

운영 프로젝트의 기본값 변경은 해당 프로젝트에 연결되어 그 값을 적용받는 다른 사용자에게도 전달될 수 있다. 팝업 모양만 확인할 때는 아래 개발자 도구의 임시 판정을 사용한다. 실제 운영 수신 검증은 [강제 업데이트 운영 스킬](../.agents/skills/chaekchaek-force-update/SKILL.md)의 기기 준비와 복원 절차를 따른다.

1. 기기 설치·서명 검사, 잠금 해제와 정상 기준 화면 확인을 먼저 끝낸다.
2. 변경 전 정책을 저장하고 일시 차단값, 대상 범위와 복원까지 승인받는다. 실패 시에도 복원을 시도하는 `finally` 경로와 제한 시간을 준비한다. 과거 검증에서는 최대 90초를 사용했다.
3. 값을 게시하고 서버 재조회 후 기기에서 강제 팝업을 확인한다. 사용자 터치와 자동 검증을 겹치지 않게 한다.
4. 확인 즉시 현재 템플릿을 다시 읽고 자신이 바꾼 정책만 이전 값으로 복원한다. 무조건 0으로 바꾸는 것이 원복은 아니다. 다른 매개변수의 동시 변경을 보존하고 같은 정책의 충돌이 있으면 덮어쓰지 않는다.
5. 서버 값이 돌아왔는지 확인한 뒤 앱에서도 팝업이 사라졌는지 확인한다. 서버 복원 성공과 앱의 복원 수신을 따로 기록한다.

기존 템플릿 전체로 롤백하면 다른 설정의 후속 변경도 되돌릴 수 있다. 자신이 바꾼 정책만 되돌리는 방식을 우선한다. 오프라인 기기는 복원 정책을 받기 전까지 이전 강제 차단을 유지할 수 있다.

## 수신, 캐시와 적용 시점

- 앱은 첫 네트워크 요청을 기다리지 않고 기본값 또는 마지막 검증된 정책으로 시작한다.
- 시작과 foreground 복귀에서 수신을 요청한다. SDK 최소 수신 간격은 1시간, 개별 네트워크 수신 제한 시간은 10초다. 반복 복귀는 SDK 캐시를 사용한다.
- foreground 동안 실시간 Remote Config 리스너를 등록한다. 변경 통지를 받은 SDK는 일반 최소 수신 간격과 별개로 새 값을 받아 온다. 백그라운드에서는 리스너를 제거한다.
- 검증에 성공한 값은 즉시 적용한다. 일반 화면의 내용을 갑자기 바꾸는 설정은 현재 없다. 긴급 차단은 현재 작업 화면 위에 안내를 올리는 방식으로 적용한다.
- 디스크에는 검증된 JSON과 실제 SDK 수신 시각만 저장한다. 실패 또는 잘못된 정책은 기본값/마지막 정상값을 유지한다. 예전 응답이 새 수신 시각보다 오래됐으면 무시한다.
- 일반 갱신만 성공하는 환경에서는 마지막 성공 수신 후 1시간이 지난 다음 시작/복귀에 반영된다. 앱이 계속 백그라운드거나 오프라인이면 시간 상한을 보장하지 않는다. 실시간 연결 중에는 SDK 변경 통지와 수신이 끝난 뒤 반영되며 고정된 초 단위 SLA는 없다.
- 실시간 연결 실패, 서버 장애와 첫 실행 오프라인은 사용자 오류 팝업을 띄우지 않고 개발자 도구의 수신 결과에 기록한다. 마지막 정상 정책이 강제 업데이트였다면 오프라인에서도 차단을 유지한다.
- 권장 안내의 `나중에`는 해당 정책에 한해 현재 프로세스 동안 유지된다. 새 정책이나 다음 프로세스에서는 다시 안내할 수 있다. 강제 정책은 무시할 수 없다.

1시간은 매번 서버에 요청하지 않도록 둔 SDK 캐시 간격이다. 긴급 변경은 foreground 실시간 수신으로 보완한다. 강제 수신은 간격을 0으로 요청하지만 네트워크 성공이나 즉시 반영을 보장하지 않는다. 마지막 수신 시각은 버튼을 누른 시각이 아닌 SDK의 실제 fetch 시각이다.

로딩 화면 뒤 적용이나 다음 실행 적용은 시작 시 일관된 UI에 유리하지만 긴급 차단을 늦춘다. 이 구현은 시작을 기다리게 하지 않고 검증 후 즉시 적용해 대응 속도를 우선했다. 원격값으로 일반 화면 배치를 바꾸는 기능을 추가한다면 같은 적용 시점을 그대로 쓰기 전에 사용자 작업 중 변경 영향을 다시 판단한다. 전략 비교는 [Firebase 로딩 전략](https://firebase.google.com/docs/remote-config/loading)을 참고한다.

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

### 개발자 도구 재현 순서

1. Debug 앱의 개발자 화면에서 `원격 설정과 업데이트` 항목을 연다. iOS는 홈의 `개발` 진입점을 사용한다.
2. `설정 다시 받기`로 서버 값을 확인한다. 이 버튼은 임시 판정을 해제하지 않으므로 출처가 `임시`이면 `임시 설정 해제`도 누른다.
3. `강제 업데이트 재현` 후 앱 화면에서 단일 버튼과 닫기 차단을 확인한다. `권장 업데이트 재현`에서는 `나중에`로 기존 화면에 복귀하는지 확인한다. `최신 버전 재현`은 안내가 없는 상태를 만든다.
4. Android의 Play 흐름 재현은 `FakeAppUpdateManager 사용`을 선택하고 앱으로 돌아가 `업데이트`를 누른다. 강제 판정은 Immediate, 권장 판정은 Flexible로 연결된다. Fake가 제시하는 새 버전은 현재 코드 + 1이므로 실제 원격 최소값이 그보다 높으면 스토어 대체 경로로 빠질 수 있다.
5. 개발자 도구의 `Fake 업데이트 수락`, `Fake 업데이트 취소`, `Fake 다운로드 완료`로 상태를 전이한다. Flexible 완료 후 앱의 `다시 시작` 안내를 확인한다. Fake는 실제 바이너리를 설치하지 않는다.
6. Android는 `실제 Play 업데이트로 복귀`, 양 플랫폼은 `임시 설정 해제`로 원격 정책 판정에 복귀한다.

이 도구는 원격 JSON 자체를 임의 편집하는 기능이 아니라 업데이트 판정을 임시로 덮어쓰는 기능이다. 최소 버전 비교와 실제 수신을 검증하려면 유효한 원격 정책을 별도로 게시해야 한다.

### 실제 구버전 Play 업데이트 확인 순서

[공식 테스트 절차](https://developer.android.com/guide/playcore/in-app-updates/test)에 따라 정책 기능이 포함된 낮은 버전을 내부 앱 공유 링크로 설치하고, 같은 applicationId·서명의 더 높은 versionCode를 업로드한다. 새 버전 공유 링크를 기기에서 열되 그 화면에서 미리 설치하지 않고 기존 앱으로 돌아온다. 테스트 계정의 앱 소유 여부도 확인한다.

권장 정책으로 Flexible 다운로드와 재시작 후 새 버전을 확인하고, 별도의 낮은 버전 테스트에서 강제 정책으로 Immediate 설치와 복귀 후 차단 해제를 확인한다. 취소 시 강제 안내가 유지되는지도 확인한다. 각 실행에 설치 전후 버전, 정책, 스토어 배포 경로와 결과를 남긴다. 아래 기록에는 이 실제 설치 완료 증거가 아직 없다.

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

`android/config/remote-config/template.draft.json`은 검토용 원본이다. 사용자 승인 후 같은 정책을 2026-10-23 16:30:50 KST에 `chaekchaek-d2b85` Remote Config 버전 1로 게시하고 다시 조회했다. 최소/최신 경계는 모두 0이며 `verificationMessage`는 `원격 설정 연결 확인`이다. 사용자를 차단하지 않는 수신 검증 정책이다. 2026-10-23에 접근 확인한 Firebase 프로젝트는 `chaekchaek-d2b85`다. 원격 검증기의 Android Release 설정도 `chaekchaek-d2b85`임을 확인했다. Android Debug는 별도 `chaekchaek-debug`를 사용하며 당시 사용한 계정의 접근은 확인되지 않았다. 두 프로젝트를 섞거나 SDK 설정을 자동으로 바꾸지 않는다.

Firebase는 기존 서비스와 원격 업데이트 정책에 사용하고 PostHog는 사용자 이벤트 분석에 사용한다. PostHog로 통합하는 전환 비용과 러닝커브를 고려해 현재는 둘을 유지한다. 이번 통합에서 PostHog 초기화와 이벤트 전달, Firebase Remote Config 연결을 함께 보존했다.

실제 Firebase 수신 UI 테스트는 게시 이후 별도로 실행한다. Xcode 테스트 프로세스에 `TEST_RUNNER_VERIFY_LIVE_REMOTE_POLICY=1`을 전달하고 `RemotePolicyUITests/testPublishedPolicyIsReceivedWithoutStoreRelease`를 실행한다. 일반 테스트에서는 외부 서비스 의존 검증을 건너뛴다. 테스트는 개발자 도구에서 강제 수신 후 원격 출처, 검증 문구와 마지막 수신 시각을 확인한다.

## 요구사항별 달성 상태

아래 판정은 구현 코드와 2026-10-23 검증 기록을 기준으로 한다. 2026-10-29 문서화에서 기기 테스트를 다시 수행한 결과는 아니다.

| 요구사항 | 상태와 근거 |
| --- | --- |
| 3. 배포 없이 값 변경과 앱 반영 | 확인. iOS 실제 수신, Android 물리기기 강제 정책 게시·수신·복원. 개별 기능 끄기 스위치는 미구현이며 현재 변경 대상은 업데이트 정책과 검증 문구다. |
| 3. 첫 실행·오프라인·장애 시 기본 동작 | 구현 및 공통 테스트 확인. 정상 캐시가 없으면 기본값, 있으면 마지막 정상 정책을 사용한다. iOS 실제 수신 실패에서도 기본값 유지를 관측했다. 모든 장애 유형의 물리기기 재현을 뜻하지는 않는다. |
| 3. 수신·적용 시점과 반영 시간 기록 | 완료. 1시간 캐시, 10초 timeout, foreground 실시간 수신, 검증 후 즉시 적용. Android 약 2.5초 관측은 SLA가 아니다. |
| 3. 개발자 도구 | 구현. 현재 값·출처·수신 시각, 다시 받기와 임시 판정 제공. JSON 전체 편집은 제공하지 않는다. |
| 4. 최소 지원 버전과 근거 | 올리는 기준과 원격 구조는 정의. 초기 경계는 0이며 실제 특정 구버전의 지원 종료 대상·기한은 미확정이다. |
| 4. 강제와 권장 안내 | 구현 및 양 플랫폼 UI 테스트 확인. Android 실제 원격 강제 안내와 복원도 확인. |
| 4. 이전 버전 설치 후 업데이트 완료 | 부분 확인. 낮은 버전으로 취급한 APK의 팝업은 확인했으나 Play Flexible/Immediate 실제 설치와 iOS App Store 업데이트 왕복은 미검증이다. |
| 4. Flexible·Immediate 개발자 재현 | Fake 조작 도구 구현. 기기에서 전체 왕복은 미검증이다. |

## 검증 상태

2026-10-23 검증 당시 `an-develop`의 PostHog 병합 커밋 `6a477fbb`를 반영했다. 당시 검증의 앱/테스트 코드 커밋은 `19873719`다. 아래 결과는 해당 검증의 기록이며 최신 브랜치 전체를 재검증했다는 의미가 아니다.

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

- [Remote Config 클라이언트 템플릿 관리](https://firebase.google.com/docs/remote-config/templates/client)
- [Remote Config 로딩 전략](https://firebase.google.com/docs/remote-config/loading)
- [Android Remote Config](https://firebase.google.com/docs/remote-config/android/get-started)
- [iOS Remote Config](https://firebase.google.com/docs/remote-config/ios/get-started)
- [Play In-App Updates 구현](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java)
- [Play In-App Updates 테스트](https://developer.android.com/guide/playcore/in-app-updates/test)
