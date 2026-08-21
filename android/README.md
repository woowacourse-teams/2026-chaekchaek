# 첵췍 Android

Android 빌드 환경, 릴리스 서명, Google Play 배포 절차는
[Android 빌드·서명 운영 가이드](../docs/android-build-signing.md)를 따른다.

현재 Play 상태, 버전 부여 규칙, 릴리스별 변경과 복구 기준은
[Android 버전 및 릴리스 관리](../docs/android-release-management.md)에서 확인한다.

## 개발 문서

- [Android CI 운영 및 학습 기록](docs/android-ci.md) - 자동 검증 범위, 실행 방법, 실패 대응
- [iOS App Store 심사 준비](docs/ios-app-store-review.md) - 빌드, 서명, 키, 제출 메타데이터

## 설계·구현 문서

작업을 이어받는다면 [구현 인수인계](docs/implementation-handoff.md)부터 읽는다. 현재 진행
상태와 다음에 할 일이 정리되어 있다.

- [구현 인수인계](docs/implementation-handoff.md) - 진행 상태, 작업 리듬, 다음 단계
- [도메인 모델](docs/domain-model.md) - 객체와 규칙, 테스트 대상 목록
- [앱 아키텍처](docs/app-architecture.md) - 레이어, DI, 화면 상태 규칙
- [KMP 셋업](docs/kmp-setup.md) - 모듈 구성, 검증된 버전 조합
- [화면 명세](docs/screen-specs.md) - 화면 12개의 상태·액션
