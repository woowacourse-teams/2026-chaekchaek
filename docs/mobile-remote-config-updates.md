# 모바일 원격 설정과 업데이트 정책

Firebase Remote Config로 Android와 iOS의 최소 지원 버전과 최신 버전을 바꾼다. 앱 배포 없이 업데이트를 강제하거나 권장할 수 있다. [관련 이슈 #581](https://github.com/woowacourse-teams/2026-chaekchaek/issues/581)

## 구현 방식

Firebase 수신은 플랫폼별로 처리하고, JSON 검증·캐시·버전 비교·팝업은 Kotlin 공통 코드로 처리한다.

- [MobilePolicy.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/config/MobilePolicy.kt): 설정 형식과 버전 비교.
- [MobilePolicyController.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/config/MobilePolicyController.kt): 마지막 정상값 저장, 수신 상태와 임시 판정.
- [AndroidRemotePolicy.kt](../android/app/src/main/java/com/chamsae/chaekchaek/AndroidRemotePolicy.kt), [iOS의 IOSRemotePolicy](../android/iosApp/Chaekchaek/ContentView.swift): Firebase 수신과 로컬 저장.
- [AndroidAppUpdates.kt](../android/app/src/main/java/com/chamsae/chaekchaek/AndroidAppUpdates.kt): Play 업데이트 연결.
- [UpdatePolicyHost.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/common/UpdatePolicyHost.kt): 공통 업데이트 팝업과 개발자 도구.

원격값은 버전 정책과 수신 확인 문구다. 스토어 주소, 검증 규칙과 UI 문구는 앱에 고정한다. 비밀 키나 개인정보는 클라이언트에서 읽을 수 있는 원격 설정에 넣지 않는다. 사용자 공지와 개별 기능 스위치는 현재 정책에 포함하지 않는다.

## 설정값

매개변수 이름은 `mobile_policy_v1`, 유형은 **JSON**이다. 최소·최신 버전을 하나의 값으로 받아 함께 검증한다.

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

위 값은 안내를 비활성화하는 예시다. 앱 내 기본값도 같지만 `verificationMessage`는 빈 문자열이다.

| 값 | 의미 |
| --- | --- |
| `schemaVersion` | 지원 형식. 현재 1 |
| `androidMinimumVersionCode` / `androidLatestVersionCode` | Android 최소 / 최신 versionCode |
| `iosMinimumVersion` / `iosLatestVersion` | iOS 최소 / 최신 버전. 예: 1.3.2 |
| `verificationMessage` | 개발자 도구에 표시하는 수신 확인 문구. 최대 UTF-16 200자 |

설치 버전이 최소보다 낮으면 **강제**, 최소 이상이면서 최신보다 낮으면 **권장**, 최신 이상이면 안내하지 않는다. Android는 `versionCode`, iOS는 `CFBundleShortVersionString`을 숫자로 비교한다.

최소 버전은 보안 문제, 치명적 크래시, API 호환성 제거로 정상 이용이 어려울 때 올린다. 일반 개선은 최신 값만 올린다. 새 버전을 대상 사용자가 스토어에서 실제 설치할 수 있는지 먼저 확인하고, API 하위 호환은 구버전 사용 현황과 업데이트 가능 여부를 고려해 유지한다. 이 정책은 정책 해석 코드가 포함된 앱부터 동작한다.

## Firebase에서 변경하기

1. 책췍 계정으로 [Firebase Console](https://console.firebase.google.com/project/chaekchaek-d2b85/config)에 접속한다. 운영 프로젝트는 `chaekchaek-d2b85`이며 Android Debug는 별도 `chaekchaek-debug`이므로 설치 앱의 연결 프로젝트를 확인한다.
2. **Remote Config > Parameters > mobile_policy_v1**을 열어 현재 값과 템플릿 버전을 보관한다.
3. JSON을 수정한다. 최신은 최소 이상이어야 하며, 한 플랫폼만 변경할 때 다른 플랫폼 값은 유지한다.
4. 변경 내용을 검토하고 저장한 뒤 **변경사항 게시**를 누른다.
5. 서버 값을 다시 조회하고 앱을 foreground로 가져와 적용 결과를 확인한다. Debug에서는 `설정 다시 받기`와 검증 문구로 수신을 확인할 수 있다.

설치 앱이 Android 코드 10, iOS 1.3.1이라고 가정한 설정 예시:

| 목적 | Android 최소 / 최신 | iOS 최소 / 최신 |
| --- | --- | --- |
| 안내 없음 | 0 / 0 | 0.0.0 / 0.0.0 |
| 새 버전 권장 | 0 / 11 | 0.0.0 / 1.3.2 |
| 구버전 차단 | 11 / 11 | 1.3.2 / 1.3.2 |

[template.draft.json](../android/config/remote-config/template.draft.json)은 전체 템플릿 형식의 참고 파일이다. 기존 매개변수와 조건을 보존하며, 초안으로 운영 템플릿 전체를 덮어쓰지 않는다.

**일시 테스트 후에는 변경 전 정책으로 복원하고, 서버 값과 앱 팝업 해제를 모두 확인한다.** 복원 직전 최신 템플릿을 읽어 다른 변경을 보존한다. 상세 기기 준비와 복원 절차는 [강제 업데이트 운영 스킬](../.agents/skills/chaekchaek-force-update/SKILL.md)을 따른다.

## 수신과 적용 시점

- 앱은 네트워크를 기다리지 않고 기본값 또는 마지막 정상 캐시로 시작한다.
- 시작과 foreground 복귀에서 수신한다. 최소 수신 간격은 **1시간**, timeout은 **10초**다.
- foreground에서는 실시간 변경도 수신해 1시간 간격을 보완하며, 검증에 성공한 값은 즉시 적용한다. 긴급 차단 대응을 위해 다음 실행까지 적용을 미루지 않는다.
- 잘못된 JSON, 음수 버전, 최신 < 최소, 미지원 스키마, 4096자 초과 값과 오래된 응답은 거절한다. 수신 실패 시 마지막 정상값을 유지하며, 캐시가 없는 첫 실행은 기본값으로 동작한다.
- 일반 갱신은 1시간이 지난 뒤 다음 시작·복귀에 반영된다. 실시간 수신은 통지와 다운로드 후 반영되며, 오프라인·백그라운드에서는 반영 시간 상한이 없다. 캐시된 강제 정책도 새 값을 받기 전까지 유지된다.

2026-10-23 Android 물리기기에서 강제 정책 게시 후 서버 재조회 완료부터 약 2.5초 뒤 팝업 표시를 확인했고, 기존 정책 복원 후 팝업 해제도 확인했다. 이 시간은 당시 관측값이다.

## 업데이트 동작과 개발자 도구

| 판정 | 사용자 안내 | Android | iOS |
| --- | --- | --- | --- |
| 강제 | `업데이트`만 표시, 뒤로가기·외부 탭으로 닫기 불가 | 가능한 경우 Immediate | App Store 이동 |
| 권장 | `나중에`, `업데이트` | 가능한 경우 Flexible | App Store 이동 |
| 다운로드 완료 | `다시 시작` | completeUpdate 호출 | 해당 없음 |

Android에서 Play 업데이트를 사용할 수 없으면 스토어로 이동한다. Immediate를 취소해도 강제 안내는 유지된다. Flexible은 다운로드 중 앱 이용을 허용하고 완료 후 재시작을 안내한다. 권장 안내의 `나중에`는 같은 정책에 대해 현재 프로세스 동안 유지된다.

Debug 개발자 화면의 `원격 설정과 업데이트`에서 현재 값·출처·마지막 수신 시각(UTC)·수신 결과를 확인한다. iOS 진입점은 홈의 `개발`이다.

- `설정 다시 받기`: 수신 간격을 0으로 요청한다.
- `강제 업데이트 재현`, `권장 업데이트 재현`, `최신 버전 재현`: 판정만 임시로 덮어쓴다. 원격값과 디스크 캐시는 바꾸지 않는다.
- `임시 설정 해제`: 실제 정책 판정으로 복귀한다.
- Android의 `FakeAppUpdateManager 사용`: 앱에서 업데이트를 시작한 뒤 개발자 도구의 수락·취소·다운로드 완료 버튼으로 상태를 전이한다. `실제 Play 업데이트로 복귀`로 해제한다. Fake는 실제 바이너리를 설치하지 않는다.

실제 Play 업데이트는 Play 설치 앱에서 확인한다. 내부 앱 공유로 낮은 버전을 설치하고 같은 applicationId·서명의 높은 versionCode를 올린 뒤, 새 버전 공유 링크를 열되 직접 설치하지 않고 기존 앱으로 돌아가 업데이트 흐름을 실행한다.

## 참고

- [Firebase Remote Config 로딩 전략](https://firebase.google.com/docs/remote-config/loading)
- [Firebase 템플릿 관리](https://firebase.google.com/docs/remote-config/templates/client)
- [Play In-App Updates 구현](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java)
- [Play In-App Updates 테스트](https://developer.android.com/guide/playcore/in-app-updates/test)
