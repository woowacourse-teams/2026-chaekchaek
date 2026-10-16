# 책췍 Android

작업 범위는 `android/`이며 [작업 규칙](AGENTS.md)을 먼저 읽는다.
문서는 [작업별 문서 인덱스](docs/document-routing.md), main 반영은 [main 반영 가이드](docs/main-integration.md)에서 시작한다.

Android 빌드 환경, 릴리스 서명, Google Play 배포 절차는
[Android 빌드·서명 운영 가이드](docs/android-build-signing.md)를 따른다.

현재 Play 상태, 버전 부여 규칙, 릴리스별 변경과 복구 기준은
[Android 버전 및 릴리스 관리](docs/android-release-management.md)에서 확인한다.

릴리스 최적화, 코드 보호, Crashlytics 설정과 난독화 오류 확인 절차는
[Android 릴리스 최적화와 코드 보호](docs/android-crashlytics.md)에 정리한다.

Google Play 프로덕션 심사 제출과 실제 사용자 관찰 과제의 수행 근거는
[Android 정식 출시 심사 및 사용자 관찰](docs/android-production-review-user-observation.md)에 기록한다.

API 서버 환경 선택과 주소 변경은 [환경 설정 안내](docs/android-api-environments.md)를 따른다.

Google 로그인 설정, 패키지명 및 Play 서명 변경 점검과 오류 진단은
[Android Google 로그인 운영 가이드](docs/android-google-login.md)를 먼저 확인한다.

## 개발 문서

- [Android CI 운영 및 학습 기록](docs/android-ci.md) - 자동 검증 범위, 실행 방법, 실패 대응
- [iOS App Store 심사 준비](docs/ios-app-store-review.md) - 빌드, 서명, 키, 제출 메타데이터

## 설계·구현 문서

발견, 피드, 도서 상세의 상태 정책과 검증 결과는 [상태 보존 구현 기록](docs/android-state-preservation-handoff.md)을 참고한다.

과거 구현 의도는 [구현 인수인계](docs/implementation-handoff.md)에서 확인한다. 당시 브랜치,
진행 상태와 Pencil 기준은 현재 작업 지시로 사용하지 않는다. 현재 작업은 위 문서 인덱스에서 시작한다.

- [구현 인수인계](docs/implementation-handoff.md) - 과거 구현과 검증 기록
- [도메인 모델](docs/domain-model.md) - 객체와 규칙, 테스트 대상 목록
- [앱 아키텍처](docs/app-architecture.md) - 레이어, DI, 화면 상태 규칙
- [KMP 셋업](docs/kmp-setup.md) - 모듈 구성, 검증된 버전 조합
- [화면 명세](docs/screen-specs.md) - 화면 12개의 상태·액션
