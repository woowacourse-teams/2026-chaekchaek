# QA2차 구현 검증 체크리스트

## 검증 기준

- 원문: [Notion QA2차](https://app.notion.com/p/3c99e827212980a482a8dd09a8ed404e)
- 원문 조회 시각: 2026-08-29 20:23 KST
- 구현 기준: `origin/an-develop@96093b107b1337f47a18a10ba4ba249950592775`
- 방법: Notion 항목을 원자 요구사항 29개로 나누고 Android 코드와 테스트를 정적 대조
- 체크 의미: `[x]`는 구현 확인, `[ ]`는 미구현, 부분 구현 또는 수동 확인 필요

Notion의 홈 C2 동기화 블록은 연동 계정에 원본이 공유되지 않아 MCP 본문에서 펼칠 수 없었다.
해당 항목은 같은 `QA2차` 문서에서 사용자가 제공한 원문인 "인기 책의 표지를 누르면 상세로
이동되지 않음"을 기준으로 검증했다. 스크린샷만 있고 목표 문구가 없는 항목은 코드만으로 완료
처리하지 않았다.

## 요약

| 판정 | 개수 |
| --- | ---: |
| 구현 확인 | 2 |
| 부분 구현 | 9 |
| 미구현 | 16 |
| 수동 확인 필요 | 2 |
| 합계 | 29 |

## 홈 페이지

| 체크 | ID | Notion 요구사항 | `an-develop` 판정 | 근거 | 로컬 QA 브랜치 |
| --- | --- | --- | --- | --- | --- |
| [ ] | H1 | 알림 아이콘 표시 | 미구현 | `HomeScreen.kt:354`의 `HomeHeader`에는 프로필만 있다. | 관련 변경 없음 |
| [ ] | H2 | 알림 아이콘 클릭 시 알림 목록 이동 | 미구현 | `HomeScreen.kt:113`에 알림 콜백이 없고 `Navigation.kt:49`에 알림 경로가 없다. | 관련 변경 없음 |
| [ ] | H3 | 인기 책 표지 클릭 시 상세 이동 | 미구현 | `HomeScreen.kt:439`, `HomeScreen.kt:487`의 표지 클릭은 선택 인덱스만 바꾼다. | 관련 변경 없음 |
| [x] | H4 | 인기 책 이름 클릭 시 상세 이동 유지 | 구현 확인 | `HomeScreen.kt:452`에서 이름 클릭이 `onBookClick`으로 이어진다. | 기존 구현 |

## 검색 페이지

| 체크 | ID | Notion 요구사항 | `an-develop` 판정 | 근거 | 로컬 QA 브랜치 |
| --- | --- | --- | --- | --- | --- |
| [ ] | S1 | 결과 목록 스크롤 시 키보드 숨김 | 미구현 | `SearchScreen.kt:273`의 목록에 포커스 해제 처리가 없다. | `77f8920`에만 구현 |
| [ ] | S2 | 상세 진입 후 복귀해도 검색어 유지 | 미구현 | `SearchScreen.kt:107`이 `remember`만 사용한다. | `77f8920`에만 `rememberSaveable` 적용 |
| [ ] | S3 | 시리즈 순서를 볼 수 있는 오름차순, 내림차순 정렬 | 미구현 | `BookSearchRepository.kt:7`과 `SearchScreen.kt:339`은 최신순, 감상 많은순만 지원한다. 로컬 OpenAPI도 두 값뿐이다. | 관련 변경 없음 |

## 상세 페이지

| 체크 | ID | Notion 요구사항 | `an-develop` 판정 | 근거 | 로컬 QA 브랜치 |
| --- | --- | --- | --- | --- | --- |
| [ ] | D1 | 앱 재실행 후 내 별점 유지 | 부분 구현 | 인증 상세 조회가 `myRating`을 복원하지만 서재 미등록 책의 저장 ID 경로가 불완전하고 재실행 통합 테스트가 없다. `BookDetailRemoteRepository.kt:26`, `BookDetailRemoteRepository.kt:371`, `BookDetailScreen.kt:311` | `77f8920`이 등록 ID 상태를 보강하고 `3063d4e`가 서재 별점 이력을 복원 |
| [ ] | D2 | 기존 별점은 신규 부여가 아닌 수정 흐름으로 표시 | 부분 구현 | 기존 값은 초기 선택값이 되고 PUT으로 덮어쓰지만 문구는 항상 "별점 주기", "새 별점"이다. `BookRatingDialog.kt:68`, `BookDetailRemoteRepository.kt:60`, `BookDetailScreen.kt:664` | 문구는 그대로 |
| [ ] | D3 | 별점 저장 직후 상세 UI에 저장값 반영 | 부분 구현 | 성공 뒤 상세를 재조회하지만 저장 응답은 버리고 다이얼로그는 요청 완료 전에 닫는다. `BookDetailViewModel.kt:145`, `BookDetailViewModel.kt:325`, `BookDetailScreen.kt:314` | `77f8920`이 저장 응답을 상태에 즉시 반영 |
| [ ] | D4 | 다른 책의 별점 창에서도 최근 내 별점 기록 노출 | 부분 구현 | 현재 세션의 최대 3건만 메모리에 보관하고 앱 시작 시 복원하지 않는다. `Navigation.kt:78`, `Navigation.kt:168`, `BookDetailModels.kt:39` | `3063d4e`에 복원 구현 |
| [ ] | D5 | 별점 부여 UI가 목표 디자인과 일치 | 수동 확인 필요 | 선택기와 최근 기록 UI는 있으나 Notion에는 목표 치수나 명시적 문구 없이 스크린샷만 있다. `BookRatingDialog.kt:60`, `BookRatingDialog.kt:197` | `77f8920`이 별 반쪽 렌더링만 수정 |
| [ ] | D6 | 5점 저장 후 4점 또는 기존 UI가 남지 않음 | 미구현 | 내 독서 기록은 저장값과 무관하게 "별점 주기"로 남고 평균 별도 `★★★★☆`로 고정된다. `BookDetailScreen.kt:648`, `BookDetailScreen.kt:628` | `77f8920`은 선택기 렌더링, `7c59e08`은 평균 별 표시만 수정해 요구사항 전체는 미완료 |
| [ ] | D7 | 답글 버튼과 맨 위로 버튼이 겹치지 않음 | 미구현 | 답글은 우측에 있고 맨 위로 버튼도 `BottomEnd` 오버레이다. `BookDetailScreen.kt:298`, `BookDetailScreen.kt:941` | `77f8920`이 답글 액션을 좌측으로 이동 |
| [ ] | D8 | 상태 선택으로 자동 등록된 책이 서재에 지연 없이 반영 | 미구현 | 상세 책 ID가 있으면 등록 여부를 확인하지 않고 PATCH하며 서재 새로고침도 호출하지 않는다. `BookDetailViewModel.kt:136`, `BookDetailViewModel.kt:332`, `Navigation.kt:166` | `77f8920`이 등록 보장과 서재 새로고침 구현 |
| [ ] | D9 | 서재에 없는 책의 별점 저장 전에 자동 등록 | 부분 구현 | UI는 별점을 허용하지만 자동 등록은 책 ID가 아예 없을 때만 실행된다. `BookDetailScreen.kt:652`, `BookDetailViewModel.kt:332` | `77f8920`이 `myRecord` 기준 등록 보장 구현 |
| [ ] | D10 | 서재 삭제가 화면에 바로 반영 | 부분 구현 | 삭제 성공 뒤 상세와 서재를 재조회하지만 응답 전 낙관적 제거는 없다. `BookDetailViewModel.kt:126`, `BookDetailViewModel.kt:325`, `Navigation.kt:163` | `77f8920`이 상세 로컬 상태 즉시 제거 |
| [ ] | D11 | 북마크 아이콘이 목표 디자인과 일치 | 미구현 | 상세 상단 버튼은 벡터 대신 `⌑` 글리프를 쓴다. `BookDetailScreen.kt:475`, `BookDetailScreen.kt:488` | `77f8920`은 쪽수 영역의 `ic_bookmark.xml`만 수정해 상단 버튼은 그대로 |
| [ ] | D12 | 감상 페이지가 없으면 날짜 뒤 중간점 생략 | 미구현 | 날짜 뒤 `·`를 항상 붙인다. `BookDetailScreen.kt:908` | 관련 변경 없음 |
| [ ] | D13 | 익명 감상 작성 후 닉네임 비노출 | 미구현 | 작성 시트는 익명으로 안내하지만 목록은 `review.anonymous`를 무시하고 `authorName`을 표시한다. `BookDetailSheets.kt:257`, `BookDetailScreen.kt:904` | 관련 변경 없음 |
| [ ] | D14 | 일정 시간 뒤에도 상세 기능 요청 정상 처리 | 부분 구현 | 세션은 토큰을 갱신하지만 상세 ViewModel은 이전 토큰을 보관하고, 이미 로그인 상태면 새 토큰을 전달받지 않는다. `AuthSession.kt:54`, `BookDetailViewModel.kt:33`, `Navigation.kt:140` | 관련 변경 없음, 만료 시간 경계 실기기 재현 필요 |

## 서재 페이지

| 체크 | ID | Notion 요구사항 | `an-develop` 판정 | 근거 | 로컬 QA 브랜치 |
| --- | --- | --- | --- | --- | --- |
| [ ] | L1 | 익명 공개 전환을 서재 상단에 상시 노출 | 부분 구현 | 현재 상태 표시는 있지만 편집 중에만 노출된다. `ArchiveScreen.kt:144`, `ArchiveScreen.kt:306` | 관련 변경 없음 |
| [ ] | L2 | 닉네임 최초 설정 뒤 익명 해제 시 설정 팝업 미노출 | 미구현 | 익명 상태면 기존 닉네임 유무와 관계없이 팝업을 연다. `ArchiveScreen.kt:157`, `ArchiveScreen.kt:247` | 관련 변경 없음 |
| [ ] | L3 | 닉네임 한글 타이핑 정상 동작 | 수동 확인 필요 | `BasicTextField`와 10자 절단만 있고 IME 조합 입력 테스트가 없다. `ArchiveScreen.kt:247`, `ArchiveScreen.kt:719` | 관련 변경 없음 |
| [x] | L4 | 설정한 닉네임을 앱 상태에 반영 | 구현 확인 | PATCH 응답을 즉시 UI 상태에 적용한다. `MemberSettingsViewModel.kt:67`, `MemberSettingsViewModel.kt:87`, `MemberSettingsViewModelTest.kt:57` | `an-develop`의 별도 회원 설정 수정에 포함 |
| [ ] | L5 | 설정한 닉네임을 서버 DB에 저장 | 부분 구현 | Android의 인증 PATCH 경로와 JSON 본문은 구현 및 테스트됐다. 실제 DB 반영은 백엔드 실행 검증이 필요하다. `MemberRemoteRepository.kt:20`, `MemberRemoteRepositoryTest.kt:20` | 관련 변경 없음 |
| [ ] | L6 | 재익명 후 다시 해제해도 닉네임 팝업 반복 미노출 | 미구현 | 저장된 닉네임을 확인하지 않는 L2와 같은 분기다. `ArchiveScreen.kt:157` | 관련 변경 없음 |

## 기타

| 체크 | ID | Notion 요구사항 | `an-develop` 판정 | 근거 | 로컬 QA 브랜치 |
| --- | --- | --- | --- | --- | --- |
| [ ] | O1 | 현재 내 서재 탭 재클릭 시 맨 위로 이동 | 미구현 | 탭 값만 다시 할당하며 서재 `listState`와 연결하지 않는다. `RootScreen.kt:149`, `ArchiveScreen.kt:120` | 관련 변경 없음 |
| [ ] | O2 | 현재 홈 탭 재클릭 시 맨 위로 이동 | 미구현 | 재클릭 이벤트를 홈으로 전달하지 않고 홈 목록 상태도 외부로 노출하지 않는다. `RootScreen.kt:149`, `HomeScreen.kt:171` | 관련 변경 없음 |

## `fix/android-qa2-20260827` 반영 상태

- 공통 기준점은 `1d8eca9`다. 로컬 QA 브랜치는 이후 `77f8920`, `3063d4e`, `7c59e08` 세 커밋을 가진다.
- 세 커밋은 `origin/an-develop`에 포함되지 않았다.
- 원격 `fix/android-qa2-20260827` 브랜치가 없고 해당 head의 GitHub PR도 없다.
- 브랜치는 현재 `origin/an-develop` 변경과 갈라져 있으므로 통째 병합보다 최신 기준에서 필요한 수정과 테스트를 선별 이식해야 한다.
- `7c59e08`은 서버 평균 평점 별 표시 수정이다. Notion의 내 별점 저장 후 UI 문제 전체를 해결한 커밋으로 보면 안 된다.

## 테스트 공백

- 정적 감사 기준 코드에서 `./gradlew :shared:allTests`가 성공했다. 이 결과는 기존 단위 테스트
  통과를 뜻하며 아래 UI와 실기기 공백을 대체하지 않는다.
- 홈 알림과 표지 클릭, 검색 키보드와 검색어 복원 Compose UI 테스트가 없다.
- 별점 재실행 복원, 저장 직후 반영, 버튼 겹침, 익명 표시, 토큰 갱신 경계 통합 테스트가 없다.
- 닉네임 PATCH 요청과 부분 성공 재시도 테스트는 있으나 IME 조합 입력과 팝업 재노출 UI 테스트가 없다.

## 객체지향 관점의 구조 문제

- `ReaderProfile.canRevealName()`과 `Nickname.isValid()` 도메인 규칙을 `ArchiveScreen`이 사용하지 않고 Boolean과 문자열로 다시 판단한다. Information Expert와 캡슐화를 위반하며 L2, L6과 검증 규칙 불일치의 직접 원인이다.
- `Navigation`이 `BookDetailViewModel.uiState.value.signedIn` 내부 상태를 조회해 인증 전이를 결정한다. Law of Demeter와 캡슐화를 위반하고 새 토큰 전달 누락으로 D14에 연결된다.
- `SearchViewModel`이 검색, 페이지네이션, 서재 등록, 로그인 판정과 로그인 후 등록 재개까지 맡는다. 단일 책임 원칙을 위반한다.
- `MemberSettingsViewModel`이 저장소 추상화가 아닌 `MemberRemoteRepository` 구현에 직접 의존한다. 의존성 역전 원칙을 위반해 서버 저장 흐름의 대체 검증을 어렵게 만든다.
