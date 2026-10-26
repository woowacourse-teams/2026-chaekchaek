# 작업별 문서 라우팅

기존 문서를 후속 작업에서 찾기 위한 인덱스다. 작업에 해당하는 행의 문서를 읽는다. 2026-10-15에 저장소의 Markdown 문서와 기존 링크를 확인했으며, 문서에 적힌 배포 상태나 구현 완료 여부를 다시 검증한 것은 아니다.

현재 사용자 결정과 프로젝트 AGENTS.md를 우선한다. 디자인은 최신 승인 Figma, API는 최신 서버 OpenAPI와 현재 코드로 확인한다. 문서의 날짜, 기준 커밋, 미검증 항목을 함께 읽고 과거 기록을 현재 상태로 단정하지 않는다.

<a id="architecture"></a>
## 설계와 동작

| 읽을 때 | 문서 | 적용 범위와 주의점 |
| --- | --- | --- |
| 책장 구조, 담기, 기본 책장 온보딩 저장, 공개·이미지 공유, 작성자별 서재 이동과 질문 이모지 변경 | [서재·책장 확정 사항](bookshelf-decisions.md) | 최근 책장 자동 선택, 책장당 30권, 중복·공개 정책과 미결 사항. 요구사항 기록이며 구현 완료 증거가 아니다. |
| 공유 모듈, 레이어, DI, ViewModel, 내비게이션 구조 변경 | [앱 아키텍처](../android/docs/app-architecture.md) | Android와 iOS의 공유 경계와 설계 근거. 실제 구조는 현재 코드와 대조한다. |
| 오류 안내, 입력 검증, 로그인 실패와 만료, 조회와 저장 실패 변경 | [모바일 공통 오류 정책](mobile-error-policy.md) | 온보딩을 포함한 양 플랫폼의 단일 오류 다이얼로그, 상태 보존과 재시도 기준. Figma 수정과 앱 구현 상태를 구분한다. |
| 책, 서재, 별점, 감상, 스포일러의 의미와 검증 규칙 변경 | [도메인 모델](../android/docs/domain-model.md) | 초기 설계와 미결정 항목을 구분하고 이후 확정된 정책을 우선한다. |
| API 설계 배경이나 앱 도메인 매핑 확인 | [API 계약 초안](api-contract.md) | 서버 구현 전 초안이다. 실제 연동에는 최신 서버 OpenAPI를 확인하고 로컬 `android/openapi3.yaml`과 대조한다. |
| 열람자 인증, 작성자 MEMBER/GUEST, 익명 표시, 소유권·로그인 유도 변경 | [인증·작성자·익명 상태표](identity-permission-matrix.md) | 세 축과 mine을 구분한다. 기준 커밋·API 확인일, 정책·정적 구현·기기 미검증을 따로 읽는다. |
| 비로그인 감상, 답글, 좋아요, 토큰, 로그인 시 기록 계승 변경 | [게스트 쓰기 권한 계획](../android/docs/plans/guest-write-permissions.md) | 서재, 별점, 독서 상태의 로그인 경계와 미결을 확인한다. 계획 자체는 구현 완료 증거가 아니다. |
| 첫 진입, 책 선택, 로그인 후 서재 저장, 로그인 버튼 변경 | [첫 책 선택 온보딩](first-library-onboarding.md) | 확정 흐름, 시안, 공급업체 버튼 제약과 검증 범위를 확인한다. |
| 홈·피드의 책 바로 담기, 저장 상태 표시와 저장·삭제 동작 변경 | [서재 저장 상호작용 계획](library-save-interaction-plan.md) | 합의한 흐름, Figma 컴포넌트, 구현과 플랫폼별 검증·미검증 상태를 확인한다. |
| 발견, 피드, 도서 상세의 회전, 탭 왕복, 상세 왕복, 프로세스 복원 변경 | [상태 보존 구현 기록](android-state-preservation-handoff.md) | 상태 목록표, 저장 수명과 원본, 구현 이유, 자동·기기 검증 및 미검증 범위를 읽는다. |

<a id="design"></a>
## 디자인과 검증

| 읽을 때 | 문서 | 적용 범위와 주의점 |
| --- | --- | --- |
| A안 책장 목록, 상세, 편집, 이미지 저장과 단일 책장 이미지 공유 적용 | [책장 확정 사항](bookshelf-decisions.md), [책장 A안 적용 핸드오프](../handoff/handoff_261020_1019.md) | 후속 결정을 우선한다. 과거 링크 공유 시안은 현재 범위에서 제외하며 시안·앱 반영과 검증을 구분한다. |
| 책 상세 질문 확정안 A와 참고 자료 오버레이 적용 | [질문 확정안 핸드오프](../handoff.md) | 최신 질문 조회 API 연결, 이모지 UI 전체 보류, 자료 유무별 탭, 개인 별점 제약과 양 플랫폼 검증 범위를 먼저 확인한다. |
| Figma 재디자인을 앱에 적용하거나 적용 범위 확인 | [재디자인 핸드오프](figma-redesign-implementation-handoff.md) | 최신 Figma와 디자인 시스템을 다시 조회한다. 핸드오프의 작성만으로 앱 반영 완료를 뜻하지 않는다. |
| 홈, 피드, 발견, 서재와 책 상세의 당김 새로고침, 추가 로딩, 목록 끝과 overscroll 변경 | [새로고침과 목록 끝 상태 설계](pull-refresh-interaction-plan.md) | Figma 상태와 공통 컴포넌트, 기존 페이지네이션 유지 결정, 요청 경쟁과 상태 보존 구현 및 플랫폼별 검증 범위. |
| 모바일 디자인 피드백의 배경 확인 | [모바일과 공통 피드백](design-feedback-mobile-common.md) | 개선 후보 기록이며 최신 승인 시안보다 우선하지 않는다. |
| 화면별 초기 상태와 액션, 이전 구현 의도 추적 | [화면 명세](../android/docs/screen-specs.md), [구현 인수인계](../android/docs/implementation-handoff.md) | 과거 Pencil 기준과 브랜치 상태가 포함돼 있다. 현재 디자인 원본으로 사용하지 않는다. |
| 디자인 원본 수집, 화면 대조, 검증 근거 작성 | [디자인 검증 시작점](../android/visual-contracts/README.md) | 작업별 원본, 상태, 플랫폼, 뷰포트를 지정한다. |
| 추가 화면 재디자인의 기존 검증 범위 확인 | [추가 화면 근거](../android/visual-contracts/418-additional-evidence.md) | 해당 작업의 Android 근거이며 iOS 실행 증거로 확대하지 않는다. |
| QA2차 지적 사항의 처리 이력과 회귀 항목 확인 | [QA2차 체크리스트](qa2-implementation-checklist.md) | 기준 브랜치와 당시 검증 기록이다. 현재 코드의 통과를 보장하지 않는다. |
| 피드와 서재의 책등 이미지 또는 표지 대체 검토 | [책등 이미지 메모](book-spine-images.md) | YES24 상품번호 기반 과거 테스트이며 전체 도서 지원이나 서버 API 구현 증거가 아니다. |

<a id="delivery"></a>
## 개발과 배포

원격 설정과 최소 지원 버전, 업데이트 안내를 변경할 때는 [모바일 원격 설정과 업데이트 정책](mobile-remote-config-updates.md)을 먼저 읽는다.

| 읽을 때 | 문서 | 적용 범위와 주의점 |
| --- | --- | --- |
| Android 작업 시작과 개발 문서 탐색 | [Android 시작 안내](../android/README.md) | 해당 디렉터리의 AGENTS.md도 적용한다. |
| KMP 모듈, Gradle 의존성, iOS 연동 변경 | [KMP 셋업](../android/docs/kmp-setup.md) | 검증 날짜가 있는 조합과 미결정 사항을 현재 빌드 설정에 대조한다. |
| JDK, SDK, AAB, 업로드 키와 팀 서명 설정 | [빌드와 서명](android-build-signing.md) | Android 배포 절차이며 비밀값을 문서나 로그에 기록하지 않는다. |
| production, development, local 서버 선택과 주소 변경 | [API 환경 설정](android-api-environments.md), [설정 파일 안내](../android/config/README.md) | Android Gradle과 iOS Xcode 설정을 확인한다. |
| GitHub Actions, Android 빌드, 테스트, Lint 실패 조사 | [Android CI](../android/docs/android-ci.md) | 실행과 실패 대응 절차 및 당시 관찰 기록을 확인한다. |
| Google 로그인 실패, OAuth, 패키지명, Play 서명 변경 | [Google 로그인 운영](android-google-login.md) | 등록 정보와 실제 배포본 로그인 성공을 별도로 확인한다. |
| R8, keep 규칙, 릴리스 크래시와 mapping 복원 | [릴리스 최적화와 Crashlytics](android-crashlytics.md) | 당시 검증과 현재 릴리스의 동작 확인을 구분한다. |
| Android 버전, 트랙, 릴리스 기록과 복구 | [릴리스 관리](android-release-management.md) | 기록의 버전과 현재 Play Console 상태를 대조한다. |
| iOS 심사, TestFlight, 심사 회신과 녹화 준비 | [App Store 심사 준비](../android/docs/ios-app-store-review.md) | 과거 제출 ID와 빌드가 포함돼 있으므로 현재 App Store Connect 상태를 확인한다. |

<a id="research"></a>
## 실험과 운영

| 읽을 때 | 문서 | 적용 범위와 주의점 |
| --- | --- | --- |
| 모바일 이벤트 추가, 분석 지표와 계측 완료 판정 | [이벤트 로깅 설계](mobile-analytics-event-plan.md) | 이벤트 계약과 실제 수신 증거를 구분한다. SDK 구현은 콘솔 수신이나 BigQuery 연결 완료를 뜻하지 않는다. |
| 정식 출시 심사 근거와 실제 사용자 관찰 기록 확인 | [심사와 사용자 관찰](android-production-review-user-observation.md) | 제출, 게시, 사용자 관찰을 각각 확인한다. |
| 웹 읽기 실험 실행, 집계 해석, 다음 실험 설계 | [웹 시작 안내](../frontend/README.md), [읽기 실험 핸드오프](../handoff/handoff_260929_1538.md) | 노출 시간과 실제 독해, 제출 수와 감상의 질을 구분한다. 2026-09-29 이력이며 현재 책장 핸드오프와 별개다. |
| 텍스트 작품 협업 후보와 연락 초안 검토 | [작가 연락 후보](author-contact-prospects.md) | 공개 계정, 현재 활동, 본문 사용 권한은 별도 확인한다. 문서에 연락 초안이 있다는 사실은 발송 승인이 아니다. |
| 레벨4 LMS 안내, 제출 필드와 Android 요구사항 증빙 확인 | [레벨4 제출 인덱스](level4-submission/README.md) | 인덱스에서 [LMS 목록](level4-submission/lms-page-catalog.md), [스프린트 1 기준](level4-submission/sprint-1-submission.md), [출시 제출 문서](level4-submission/android-play-store-release.md), [개발자 도구와 상태 보존 제출 문서](level4-submission/android-developer-tools-state-preservation.md)로 이동한다. 마감과 제출 상태는 LMS에서 확인한다. |

## 관리 범위

루트와 하위 README, `docs`, `android/docs`, 디자인 검증 문서, 핸드오프를 대상으로 연결했다. 내용이 없는 `backend/README.md`는 읽기 경로로 추가하지 않았다. AGENTS.md와 CLAUDE.md, 스킬 및 그 참고 파일, PR 템플릿은 작업 지침 또는 도구 자산이므로 문서 목록에 중복 등록하지 않는다.

지속 보관할 문서를 추가하거나 갱신하면 `document-memory-routing` 스킬로 해당 행과 AGENTS.md의 읽기 조건을 함께 점검한다. 기존 분류에 해당하면 이 인덱스의 행을 갱신하고, 새 작업 주제가 생기면 AGENTS.md에도 진입 조건을 추가한다.
