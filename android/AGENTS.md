# Android 빌드·서명 환경 팀 소유 요구 사항 (필수)

"제 컴퓨터에서는 빌드되는데요" 문제와 keystore가 한 사람 로컬에만 있어 그 사람이 빠지면
배포가 멈추는 문제를 막기 위한 요구 사항. 반드시 지킬 것.

## 도착 조건 (완료 기준)
- 팀원 누구나 문서를 따라 릴리스 서명이 붙은 빌드 결과물을 만들 수 있다.
- minSdk / targetSdk 를 몇으로 정했는지와 그 이유가 문서화되어 있다.
- keystore와 비밀번호가 특정 팀원 1인의 로컬에만 있지 않고, 2인 이상이 접근 가능한 곳에
  보관되어 있다.

## 학습 조건 (팀원이 설명할 수 있어야 함)
- Gradle 빌드 구성(빌드 타입, 서명 설정, 의존성 관리)을 직접 작성하고 각 설정이 무엇을
  하는지 설명할 수 있다.
- minSdk 결정이 지원 기기 범위와 사용 가능한 API를 어떻게 바꾸는지 설명할 수 있고,
  targetSdk 가 스토어 정책과 연결된다는 것을 안다.
- 업로드 키와 앱 서명 키의 차이를 이해하고, 키를 잃었을 때 무엇이 불가능해지는지
  설명할 수 있다.

## SDK 버전 결정 (2026-08-03)

- **minSdk 26** (Android 8.0). 근거: apilevels.com(2026-05-28 갱신, 2026년 4월 Statcounter
  데이터) 기준 누적 기기 커버리지 96.1%. 첵췍은 일반 대중을 폭넓게 타겟하므로 최신 API
  활용보다 기기 커버리지를 우선.
- **targetSdk 36** (Android 16). 근거: Google Play 정책상 2026-08-31부터 신규 앱은
  targetSdk 36 이상이어야 제출 가능(연장 시 2026-11-01까지). 선택의 여지가 없어 정책
  최소치로 고정.
- 출처: [Target API level requirements - Play Console Help](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en),
  [API Levels - apilevels.com](https://apilevels.com/)

## 샘플앱: 검색·등록·아카이브 (2026-08-03)

리뷰 심사 기간 단축 검증용 최소 샘플앱. 도서 검색은 알라딘(Aladin) Open API를 안드로이드
앱에서 직접 호출하고(로그인 불필요), 등록/아카이브는 로그인 없이 기기 로컬(SharedPreferences)
에만 저장한다. potatok(na-archive) 백엔드의 아카이브 API는 카카오 로그인 세션이 필수라
이번 샘플 범위에서는 쓰지 않음(검토 이력: 검색만 공개 API, 등록/목록은 401 확인).

- **알라딘 TTBKey는 keystore와 동일한 취급 대상이다**: `android/local.properties`
  (`aladin.ttbkey=...`, gitignore됨)에만 존재해야 하고, 절대 커밋·코드·로그에 평문으로
  남기지 않는다. 팀원 온보딩 시 `android/local.properties.example`을 복사한 뒤 팀 공유
  보관소(비밀번호 관리자 등)에서 실제 키를 받아 채운다 - keystore와 마찬가지로 특정
  팀원 1인의 로컬에만 있으면 안 된다(위 "도착 조건" 3번째 항목과 동일 원칙).
- 앱은 `BuildConfig.ALADIN_TTB_KEY`로 키를 읽는다(`app/build.gradle.kts`가
  `local.properties`를 읽어 주입). 릴리스 APK를 디컴파일하면 이 키 문자열이 노출된다는
  한계를 인지하고 있음 - 서버 프록시로 전환하면 해소되나 이번 샘플 범위에서는 보류.
