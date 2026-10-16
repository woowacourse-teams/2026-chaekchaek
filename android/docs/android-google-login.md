# Android Google 로그인 설정과 실패 진단

## 목적과 먼저 읽을 작업

Google 로그인 실패 조사, 패키지명 변경, 빌드 변형 추가, OAuth 클라이언트 변경,
Play 앱 서명 키 변경 또는 Android 배포 검증 전에 이 문서를 읽는다.
패키지명 수정이나 AAB 재업로드만으로 OAuth 서명 등록이 자동 갱신되지는 않는다.

## 설정 원본과 실행 경로

| 확인 대상 | 원본 |
| --- | --- |
| 실제 앱 ID와 버전 | `android/app/build.gradle.kts`의 `applicationId`, 빌드 변형 및 생성된 배포 파일 |
| Android OAuth 등록 | [GCP Google 인증 플랫폼 클라이언트](https://console.cloud.google.com/auth/clients?project=chaekchaek-505207) |
| Google ID 토큰 요청용 웹 클라이언트 | `android/app/src/main/res/values/strings.xml`의 `google_web_client_id`와 같은 GCP 프로젝트의 웹 OAuth 클라이언트 |
| Play가 배포하는 서명 | [Play 앱 서명](https://play.google.com/console/u/0/developers/9150426132748430663/app/4974538051578492916/keymanagement)의 현재 및 이전 앱 서명 키 |
| OAuth 공개 상태와 테스트 사용자 | GCP Google 인증 플랫폼의 대상 메뉴 |
| 서버의 토큰 검증 설정 | 실제 운영 서버 설정. 이 저장소의 `backend`에는 현재 구현 소스가 없어 여기서 검증 완료로 판단하지 않는다 |

Android OAuth 클라이언트는 실제 패키지명과 서명 인증서 SHA-1의 조합으로 등록한다.
앱 코드가 ID 토큰을 요청할 때는 Android 클라이언트 ID 대신 웹 클라이언트 ID를 사용한다.
Firebase 분석/Crashlytics 설정 파일의 프로젝트 번호 차이만으로 로그인 실패를 단정하지 않는다.

실행 경로:

1. `android/app/src/main/java/com/chamsae/chaekchaek/MainActivity.kt`에서 로그인 콜백 호출
2. 같은 디렉터리의 `auth/GoogleIdTokenRequester.kt`에서 Credential Manager로 Google ID 토큰 요청
3. `android/shared/src/commonMain/kotlin/com/chaekchaek/app/auth/AuthViewModel.kt`에서 서버 로그인 시작
4. `android/shared/src/commonMain/kotlin/com/chaekchaek/app/data/remote/MobileAuthRemoteRepository.kt`에서 `POST /api/v1/auth/mobile/google` 호출

## 변경과 배포 전 확인

1. 배포 파일의 앱 ID와 버전을 확인한다. `.integration` 등 suffix가 붙은 변형은 별도 패키지다.
2. Play 앱 서명 화면에서 현재 키, 이전 키 및 양자 내성 서명 키가 있으면 해당 지문을 확인한다.
   업로드 키나 로컬 `signingReport` 결과만으로 Play 설치본의 서명을 판단하지 않는다.
3. 지원하는 기기에 적용되는 각 서명 SHA-1과 실제 패키지명 조합이 같은 GCP 프로젝트에
   Android OAuth 클라이언트로 등록되어 있는지 확인한다. 새 키는 별도 클라이언트로 추가하고
   이전 키가 사용되는 동안 기존 등록도 유지한다.
4. 앱의 웹 클라이언트 ID가 같은 프로젝트의 웹 OAuth 클라이언트와 일치하는지 확인한다.
   서버까지 도달한 오류라면 운영 서버가 허용하는 ID 토큰 audience도 확인한다.
5. GCP 대상 메뉴에서 게시 상태와 실제 요청 scope에 따른 테스트 사용자 제한을 확인한다.
   `openid`, `email`, `profile`만 요청하는 기본 로그인에는 테스트 사용자 제한 예외가 있으므로
   테스트 중이라는 표시만으로 실패 원인을 단정하거나 공개 상태를 임의 변경하지 않는다.
6. 콘솔 저장 후 클라이언트를 다시 열어 패키지명과 SHA-1을 확인한다. 콘솔 안내상 반영은
   5분에서 몇 시간이 걸릴 수 있다. 서명 등록만 수정한 경우 앱의 웹 클라이언트 ID가 같으면
   이 수정 때문에 AAB를 다시 빌드할 필요는 없다.
7. Play에서 설치한 실제 배포본으로 계정 선택, ID 토큰 발급, 서버 로그인, 인증 후 동작까지
   확인한다. 디버그 실행이나 계정 선택 창 표시만으로 배포본 로그인을 통과 처리하지 않는다.
8. Android 버전, 앱 버전/코드, 설치 경로, 실제 서명 지문, 검증한 단계와 미확인 단계를 기록한다.

문서만으로 재발을 보장할 수 없다. 패키지나 서명 키 변경 시 위 점검을 배포 완료 기준에 포함한다.

## 실패 문구별 조사 경로

| 표시 문구 | 현재 코드의 실패 위치 | 다음 확인 |
| --- | --- | --- |
| Google 로그인을 완료하지 못했어요. | `MainActivity`의 Google ID 토큰 요청 또는 파싱 실패 | 실제 패키지/서명, Android OAuth 등록, 웹 클라이언트, Credential Manager 예외 타입과 코드 |
| 로그인에 실패했어요. 다시 시도해 주세요. | `AuthViewModel`의 서버 로그인 또는 후속 처리 실패 | HTTP 상태/오류 코드, 서버 audience 검증, 응답 파싱, 로그인 후 처리 |

계정 선택 창이 닫힌 것만으로 성공이나 실패 위치를 판정하지 않는다.
현재 플랫폼 콜백은 원래 예외를 일반 문구로 바꾸므로 화면만으로 정확한 예외를 알 수 없다.
서버 HTTP 실패는 `Mobile auth google failed: status=..., code=...` 로그가 남을 수 있다.
진단 기록에는 예외 타입/코드와 HTTP 상태만 남기며 ID 토큰, 액세스 토큰,
리프레시 토큰, 비밀번호와 OAuth client secret은 출력하거나 문서에 저장하지 않는다.

## 2026-10-03 확인 및 조치

- 사용자 보고: Play 설치본 `1.2.1`, Android 16에서 계정 선택 후
  `Google 로그인을 완료하지 못했어요.` 표시.
- Play Console 확인: 프로덕션 `7 (1.2.1)` 출시율 100%.
- 로컬 1.2.1 AAB 앱 ID는 `com.chamsae.chaekchaek`, 웹 클라이언트 ID는
  같은 GCP 프로젝트의 책췍 Web과 일치.
- 기존 GCP 운영 Android 클라이언트는 이전 일반 서명 키 SHA-1을 등록하고 있었다.
- 현재 Play 앱 서명 SHA-1은 기존 Android 클라이언트 3개에 등록되어 있지 않았다.
- 새 클라이언트 생성 후 상세 화면을 다시 열어 패키지명과 SHA-1 저장값을 확인했다.
- GCP 대상은 외부/테스트 중이다. 기본 로그인 scope의 테스트 제한 예외를 고려해
  이 상태만으로 추가 실패를 단정하지 않았으며 공개 상태는 변경하지 않았다.

| 서명 구분 | SHA-1 | 확인 상태 |
| --- | --- | --- |
| 현재 Play 앱 서명 | `2E:8F:94:DB:C1:3A:96:3A:3D:8F:1C:48:7C:A1:EF:EB:61:FF:FE:E0` | 새 Android OAuth 클라이언트 생성 |
| 이전 일반 앱 서명 | `F5:79:42:2D:E6:3A:76:1A:33:FB:0B:F5:8C:BB:3A:97:F5:E9:60:18` | 기존 운영 Android 클라이언트 등록 유지 |
| 업로드 키 | `BE:94:81:DC:F5:22:F8:AC:A1:D0:A1:13:2E:40:FC:8D:88:CB:5B:5A` | 테스트 APK 클라이언트에 등록, Play 배포 서명 대체 근거로 사용하지 않음 |

새 클라이언트 이름: `책췍 Android Production - Play 현재 서명`.
클라이언트 ID: `845997769791-p2ej79463ohbq0f0ra7nqm9vuf0khpee.apps.googleusercontent.com`.
OAuth 클라이언트 ID와 인증서 지문은 공개 식별값이며 비밀키가 아니다.
실제 기기가 연결되어 있지 않아 설치본 서명, 원래 예외 및 수정 후 로그인 성공은 미확인이다.
따라서 새 지문 누락은 확인된 설정 결함이며 이번 실패 원인이라는 판단은 유력한 추론이다.

위 값은 이 날짜의 기록이다. 후속 작업에서는 반드시 콘솔과 설치본을 다시 확인한다.

## 공식 근거

- [Google Play services 클라이언트 인증](https://developers.google.com/android/guides/client-auth): Google 로그인의 SHA-1 요구 및 업로드/배포 서명 차이
- [Play 앱 서명](https://support.google.com/googleplay/android-developer/answer/9842756): 키 업그레이드, 플랫폼별 적용과 API 제공자에 지문 등록
- [Android Google 로그인 구현](https://codelabs.developers.google.com/sign-in-with-google-android): Android 및 웹 OAuth 클라이언트 설정
- [OAuth 대상 관리](https://support.google.com/cloud/answer/15549945?hl=en): 기본 로그인 scope의 테스트 사용자 제한 예외
