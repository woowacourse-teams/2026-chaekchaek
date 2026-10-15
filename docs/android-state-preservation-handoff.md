# Android 상태 보존 구현 기록

2026-10-15 기준. [이슈 #509](https://github.com/woowacourse-teams/2026-chaekchaek/issues/509), [Draft PR #510](https://github.com/woowacourse-teams/2026-chaekchaek/pull/510).

최소 3개 화면의 상태를 수명과 크기에 맞게 보존한다. 구현 대상은 발견, 피드, 도서 상세다. 작업은 `509-state-preservation` 전용 워크트리에서 진행했다. 아래 표는 실제 앱에 화면이 3개뿐이라는 뜻이 아니다.

## 화면 조사와 구현 범위

| 현재 화면 | 확인한 상태와 저장 방식 | 이번 범위 |
| --- | --- | --- |
| 홈 | 인기 책 선택과 목록 위치는 rememberSaveable 계열, HomeViewModel은 remember로 생성 | 조사만 수행 |
| 피드 | 서버 목록·페이지는 ViewModel, 목록 위치·스포일러·답글 초안은 아래 정책으로 연결 | 구현 |
| 발견 | 입력과 실행 검색어·정렬은 SavedStateHandle, 결과는 ViewModel, 최근 검색어는 기존 저장소 | 구현 |
| 내 서재 | 필터·정렬·책등 옵션·목록 위치는 saveable, 선택·삭제 대상은 remember, ArchiveViewModel은 remember로 생성 | 조사만 수행 |
| 도서 상세 | 목적지 인자·감상 조건·목록 위치·입력창·작업 대상 ID와 초안 | 구현 |
| 마이페이지 | 닉네임·탈퇴 창 열림은 remember, MemberSettingsViewModel은 remember로 생성 | 조사만 수행 |

Root의 4탭과 Navigation의 도서 상세·마이페이지로 주요 화면 6개를 확인했다. 평점·쪽수·감상·답글·로그인·삭제 창은 화면 안의 상태로 구분한다. BookRegisterDialog는 정의가 있으나 프로덕션 호출 경로를 찾지 못해 현재 접근 가능한 별도 화면으로 세지 않았다.

이번 변경은 Android 상태 보존과 공통 코드의 호환성 확인까지다. 홈·내 서재·마이페이지의 전체 복원 보장, 새로운 DB, 백엔드 API, 시각 디자인 변경, 스토어 배포는 포함하지 않는다. Root의 탭 상태 보관은 다른 탭의 기존 saveable 값에도 적용되지만 해당 화면 전체의 검증 완료를 뜻하지 않는다.

## 상태 목록표

'프로세스 종료까지'는 시스템이 백그라운드 프로세스를 종료하고 사용자가 기존 작업으로 복귀하는 경우다. 강제 종료, 최근 앱에서 작업 삭제, 새로운 작업으로 앱 시작까지 보장한다는 뜻은 아니다.

| 화면 | 상태 | 살아남아야 하는 상황 | 수단 | 이유 |
| --- | --- | --- | --- | --- |
| 발견 | 입력 중인 검색어 | 구성 변경·탭/상세 왕복·시스템 프로세스 종료 | SavedStateHandle | 사용자가 아직 제출하지 않은 짧은 문자열을 잃지 않는다. 기존 100 code point 제한을 적용한다 |
| 발견 | 마지막 실행 검색어·정렬 | 구성 변경·탭/상세 왕복·시스템 프로세스 종료 | SavedStateHandle | 입력 B와 실행 A를 분리해 A의 결과를 다시 조회한다 |
| 발견 | 결과 목록·진행 중 요청 | 구성 변경·탭/상세 왕복 | Root entry의 ViewModel | 큰 응답을 Saved State에 담지 않는다. 프로세스 종료 후 조건으로 다시 조회한다 |
| 발견 | 받은 마지막 페이지 번호 | 시스템 프로세스 종료 | SavedStateHandle의 Int | 첫 페이지부터 읽던 구간을 다시 확보하기 위한 작은 단서다 |
| 발견 | 결과 목록 위치 | 구성 변경·탭/상세 왕복·시스템 프로세스 종료 | Root의 LazyListState Saver | 로딩·탭 분기로 목록이 빠져도 위치를 유지한다. 새 검색·정렬 변경·검색 종료 시 처음으로 이동한다 |
| 발견 | 최근 검색어 | 화면을 닫거나 앱을 새로 시작해도 | 기존 SharedPreferences | 사용자가 다시 찾을 이력이다. 복원 조회에서는 이력을 재기록하지 않는다 |
| 피드 | 목록·페이지·조회 상태 | 구성 변경·탭/상세 왕복 | Root entry의 ViewModel | 응답은 메모리 캐시이며 프로세스 복원 후 서버에서 다시 읽는다 |
| 피드 | 받은 마지막 페이지 번호 | 시스템 프로세스 종료 | SavedStateHandle의 Int | 읽던 항목과 답글 대상이 있는 구간까지 다시 조회한다 |
| 피드 | 목록 위치 | 구성 변경·탭/상세 왕복·시스템 프로세스 종료 | Root의 LazyListState Saver | 목록과 별도로 위치 단서만 저장한다 |
| 피드 | 펼친 스포일러 ID·답글 대상 ID | 구성 변경·탭/상세 왕복·시스템 프로세스 종료 | 탭 SaveableStateHolder 안의 rememberSaveable | 전체 감상 객체 대신 기본형 ID를 저장하고 최신 목록에서 대상을 찾는다 |
| 피드 | 작성 중인 답글 | 구성 변경·시스템 프로세스 종료 | 기존 ReplyInputSheet의 rememberSaveable | 입력 문자열만 보관한다. 창을 닫거나 피드를 뒤로 닫으면 버린다 |
| 도서 상세 | 도서 식별자와 표시용 짧은 메타데이터 | 구성 변경·시스템 프로세스 종료 | 직렬화 가능한 Navigation 3 key | 상세 조회를 다시 시작한다. 응답 전체·본문·이미지 바이트는 넣지 않는다 |
| 도서 상세 | 감상 범위·정렬·받은 마지막 페이지 번호 | 구성 변경·시스템 프로세스 종료 | SavedStateHandle | 서버 감상을 같은 조건과 구간으로 재조회한다. 비회원은 MINE을 ALL로 전환한다 |
| 도서 상세 | 도서 응답·감상·조회 상태 | 구성 변경 | 상세 entry의 ViewModel | 상세를 pop하면 해제하며, 프로세스 종료 후 응답을 재조회한다 |
| 도서 상세 | 목록 위치·펼친 스포일러 ID | 구성 변경·시스템 프로세스 종료 | rememberLazyListState·rememberSaveable | 조회를 마치기 전에 임시로 짧은 목록을 배치해 복원 offset을 덮어쓰지 않는다 |
| 도서 상세 | 평점·쪽수·감상·답글 창 열림과 초안 | 구성 변경·시스템 프로세스 종료 | rememberSaveable | 열림 여부와 기본형 입력값만 저장한다. 평점은 Rating 객체 대신 halfStars Int다 |
| 도서 상세 | 관리·수정·삭제·답글 대상 | 구성 변경·시스템 프로세스 종료 | rememberSaveable의 ID | 재조회한 최신 응답에서 찾는다. 삭제되거나 조회할 수 없는 대상의 창은 열지 않는다 |
| 세 화면 | 등록·좋아요·삭제·제출·인증 후 대기 액션 | 자동 재실행하지 않음 | 저장하지 않음 | 복원으로 중복 쓰기를 발생시키지 않는다. 실제 쓰기는 사용자 동작에서만 시작한다 |

발견은 기존 뒤로가기의 clear 정책을 유지한다. 피드는 뒤로가기로 홈에 돌아갈 때 탭의 saveable UI 상태와 목록 위치를 지운다. 단순 탭 전환과 상세 왕복은 닫기로 취급하지 않는다. 도서 상세는 pop하면 entry의 ViewModel과 저장 상태를 해제한다. 감상 초안은 기존 '작성 취소' 확인을 거쳐 버린다. 영구 임시 저장 기능을 추가한 것은 아니다.

## 저장 수명과 원본

| 수단 | 구성 변경 | 시스템 프로세스 종료 후 기존 작업 복귀 | 사용자가 화면을 닫고 새로 열기 |
| --- | --- | --- | --- |
| ViewModel | 해당 owner가 유지되는 동안 보존 | 소멸, 새 인스턴스 생성 | owner가 끝나면 소멸. Root 탭은 명시한 초기화 정책 적용 |
| SavedStateHandle·rememberSaveable | 복원 | 시스템에 저장된 작은 단서로 복원 | 해당 화면 상태를 지우거나 목적지를 pop하면 폐기 |
| SharedPreferences 등 영구 저장소 | 보존 | 보존 | 보존. 앱 데이터 삭제·삭제 정책은 별개 |

서버가 도서·감상·피드 응답의 원본이며 ViewModel은 조회 캐시다. 검색 조건과 상세 감상 조건은 SavedStateHandle에 기록하고 StateFlow를 화면용 표현으로 사용한다. 목록 위치·입력창 내부 초안은 UI가 소유한다. 최근 검색어의 원본은 SharedPreferences이며 화면 목록은 이를 읽은 표현이다. 같은 값을 UI와 ViewModel 양쪽에서 독립적으로 수정하는 원본으로 두지 않는다.

Saved State에는 검색어, enum 이름, 페이지 번호, ID, offset, 제한된 입력 문자열을 둔다. 인증 토큰·전체 응답·결과 목록·이미지는 넣지 않는다. 저장은 Activity가 정지할 때 시스템이 기록하는 시점의 영향을 받으므로 영구 저장의 대체물이 아니다.

## 변경 내용과 이유

- Navigation 3에 saveable state와 ViewModelStore decorator를 함께 연결했다. Search/Feed는 Root entry, BookDetail은 상세 entry에서 `viewModel()`과 `createSavedStateHandle()`로 생성한다. 단순 `SavedStateHandle()` 생성은 실제 앱의 복원 연결로 사용하지 않는다.
- KMP commonMain에서 사용하는 lifecycle Compose와 Navigation 3 모듈을 JetBrains 멀티플랫폼 좌표로 연결했다.
- Root에 탭별 SaveableStateHolder와 피드·검색 목록 state를 두었다. Composable 제거와 화면 종료를 구분하기 위해서다.
- 검색 복원은 사용자 제출과 분리해 최근 검색어와 제출 분석 이벤트를 중복 기록하지 않는다. clear는 저장된 입력과 실행 조건도 지운다.
- 목록 전체 대신 마지막 성공 페이지를 저장한다. 복원은 첫 페이지부터 순서대로 조회하고 ID 중복을 제거한다. 서버가 다음 페이지를 주지 않거나 다음 번호가 증가하지 않으면 중단한다. 실패하면 자동 반복하지 않고 기존 오류·재시도 흐름을 사용한다.
- 상세의 전체 감상·답글 객체 참조를 저장 가능한 ID로 교체했다. 복원 시 조회된 항목이 없으면 오래된 객체로 수정·삭제 창을 열지 않는다.
- 수명이 길어진 ViewModel이 이전 Activity의 인증·등록 콜백을 붙잡지 않도록 현재 콜백을 다시 연결한다. 구성 변경만으로 같은 도서·인증 상태의 상세 조회를 다시 시작하지 않는다.

객체지향 관점의 남은 책임 집중: `AppNavigation`은 의존 객체 생성, 인증 동기화, 목적지 이동을 함께 담당해 단일 책임 원칙에 어긋나는 변경 이유가 모여 있다. 이번에는 상태 소유권과 콜백 수명에 필요한 부분만 바꾸고 전체 구조 분리는 하지 않았다.

## 검증 결과

검증 환경은 전용 AVD `chaekchaek_state_509`, `emulator-5580`, Android 13(API 33), arm64, 480x960이다. 다른 세션의 에뮬레이터는 이 검증에 사용하지 않았다. 시스템 종료는 HOME으로 백그라운드 전환 후 `adb -s emulator-5580 shell am kill com.chamsae.chaekchaek`을 실행하고 기존 task를 앞으로 가져왔다. 강제 종료나 task 삭제로 대체하지 않았다.

| 화면 | 구성 변경 직접 확인 | 시스템 프로세스 종료 직접 확인 | 화면 닫고 재열기 직접 확인 |
| --- | --- | --- | --- |
| 발견 | 회전·다크 모드 후 입력 HarryDraft509와 제출 Harry의 결과 2,545건을 구분해 유지 | PID 6127 -> 없음 -> 7863. 같은 입력·실행 결과 복원 | 뒤로 닫고 발견 재진입 시 입력·결과 초기화, 최근 검색 Harry 유지 |
| 피드 | 회전·다크 모드 후 feed-draft-509 답글 유지. 탭·상세 왕복 후 위치·펼친 스포일러 유지 | PID 8415 -> 없음 -> 8936. 답글 창·초안 복원, 닫은 뒤 같은 목록 위치와 펼친 본문 확인 | 답글 창을 닫고 피드에서 뒤로 홈 이동 후 재진입 시 첫 항목 사양부터 표시 |
| 도서 상세 | 너의 이름은.의 감상 detail-draft-509가 회전·다크 모드 후 유지 | PID 7863 -> 없음 -> 8415. 상세 목적지·감상 창·초안 복원 | 작성 취소 확인 후 상세 pop·재진입, 새 감상 창은 빈 입력 |

위 표는 기본 시나리오다. 페이지 복원 보완 후 구현 커밋 `d130828d`의 APK에서도 다음을 추가 확인했다.

| 화면 | d130828d APK의 프로세스 종료 결과 |
| --- | --- |
| 발견 | PID 12580 -> 없음 -> 13337. HarryDraft509 입력, Harry 결과 2,545건, 감상 많은 순, 불사조 기사단 2·죽음의 성물 3이 보이는 위치 유지. 회전·다크 모드도 재확인 |
| 피드 | PID 10535 -> 없음 -> 12580. 스크롤 후 애정 행각·이방인·자살토끼의 표시 텍스트와 좌표가 종료 전후 일치 |
| 도서 상세 | PID 13337 -> 없음 -> 13827. 노르웨이의 숲의 final-detail-509 초안 유지. 작성 취소 후 돌아온 감상·답글 목록의 표시 텍스트와 좌표도 종료 전후 일치. 회전·다크 모드도 재확인 |

최신 `an-develop`의 `c7a6c336`을 작업 브랜치에 병합했다. 같은 상세 화면에 추가된 삭제 콘텐츠 숨김과 조회 실패 재시도를 유지했고, 저장된 대상 ID도 삭제되지 않은 최신 항목에서만 찾는다. 복원 조회 실패 시 `isRestoring`을 해제해 기존 재시도 UI로 복구할 수 있게 했다.

통합 APK에서 노르웨이의 숲의 `merged-detail-509` 감상 초안을 회전·다크 모드 후 확인했다. PID `15029 -> 없음 -> 15469`인 시스템 종료 후 같은 창·초안을 복원했고, 작성 취소 후 감상 목록의 텍스트와 좌표도 종료 전과 일치했다. 상세를 pop하고 같은 도서를 다시 열어 새 감상 입력이 빈 값인지 확인했다.

통합 후 자동 검증은 다음 명령이 모두 성공했다.

- `:shared:testAndroidHostTest`: 227개, 실패·오류 0개. 검색 입력/실행 분리, clear 후 재복원 방지, 이력/분석 이벤트 중복 방지, 페이지 재조회와 중복 제거, 피드 복원 실패 후 명시적 재시도, 상세 조건·페이지 복원과 대기 액션 미재생, 복원 조회 실패 후 재시도를 포함한다.
- `:app:assembleDebug :app:lintDebug`: 성공.
- `:app:testDebugUnitTest`: NO-SOURCE. 앱 모듈 단위 테스트가 실행됐다는 의미로 세지 않는다.
- `:shared:compileKotlinIosSimulatorArm64`: 성공.
- `:app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest`: 성공.
- `BookDetailPaginationRetryTest`: 전용 `emulator-5580`에서 2개 통과. 페이지·새로고침 실패의 명시적 재시도, 중복 요청 방지, 로딩 표시, 삭제 감상 숨김을 확인했다. 테스트 UI 상태를 주입한 검증이며 실제 서버 장애 재현은 아니다. 로그는 `/tmp/chaekchaek-509-instrument-results.log`다.

통합 첫 실행에서는 기존 `MemberSettingsViewModelTest.partialSuccessIsAppliedAndRetrySendsOnlyIncompleteSetting`이 `Dispatchers.resetMain()` 이후 남은 코루틴 접근으로 실패했다. 동일한 전체 명령 재실행에서 227개가 통과했으며, 이 간헐적인 테스트 정리 문제를 상태 복원 회귀로 처리하거나 수정 범위에 포함하지 않았다.

통합 검증 APK SHA-256: `2c20113dd6c618f599d46c083ec8a93cf08176ced77fc3b7c37830c99603dbf9`. 빌드 로그는 로컬 `/tmp/chaekchaek-509-gradle-merged-rerun.log`다. 병합 전 APK 해시는 `656902015e9b62225b9d7811cd0612cf647d6a7517102da1ba81323abc3eab91`이며 로그는 `/tmp/chaekchaek-509-gradle-final.log`다. 기기에서 실제 감상·답글 제출, 좋아요·삭제는 수행하지 않았다.

기기 관찰 원본은 로컬 `/tmp/chaekchaek-509-evidence/`의 화면명별 PNG와 UIAutomator XML이다. 입력·PID·관찰값은 위 표에 남겨 임시 파일 없이도 결과를 읽을 수 있게 했다. 회원 로그인 후 평점·쪽수 편집, 프로세스 복귀 시 비동기 인증 복원과 MINE 필터 유지, 게스트 소유 답글 수정·삭제 확인창, 네트워크 실패 중 실제 서버 UI 왕복은 기기에서 미검증이다. 테스트와 컴파일을 해당 기기 검증의 대체로 취급하지 않는다. iOS는 컴파일 확인 범위이며 시뮬레이터 UI는 미검증이다.

조회 시점의 서버 항목 추가·삭제·정렬 변화까지 과거 화면을 그대로 재현하지는 않는다. 목록은 재조회한 순서에 index·offset을 적용하므로 대상이 사라졌으면 확보된 목록 범위로 보정된다. 스포일러·대상 ID도 서버에 없는 항목을 복구하거나 쓰기를 재실행하지 않는다.

## PR 리뷰 후 인증과 평점 복원 보완

2026-10-15 독립 리뷰에서 발견한 인증 복원과 토큰 갱신 경계를 수정했다.

- 저장된 갱신 토큰으로 회원 인증을 복원하는 동안의 `null`을 확정된 비회원으로 처리하지 않는다. 피드의 최초 조회와 쓰기, 상세의 최초 조회와 편집 UI는 인증 복원 결과를 기다린다. 401이면 비회원으로 전환하며 일시적인 서버 오류는 기존 재시도 정책으로 기다린다.
- 같은 회원의 액세스 토큰 갱신은 상세의 MINE 조건과 상세·피드의 받은 페이지 번호를 유지한다. 로그아웃은 페이지와 회원 전용 조건을 초기화한다. 토큰에는 별도의 회원 식별자가 없으며 계정 교체는 앱의 로그아웃 후 로그인 경로를 전제로 한다.
- 피드는 토큰 갱신 조회 중 기존 목록을 유지해 답글 창과 초안을 제거하지 않는다. 갱신 조회 중 중복 페이지 요청을 막고, 취소된 페이지 요청의 로딩 상태를 해제한다.
- 별점 창은 사용자가 편집하지 않았다면 늦게 도착한 기존 평점을 반영한다. 사용자가 조절했거나 저장 상태에서 복원한 편집값은 서버 응답으로 덮어쓰지 않는다.

추가 회귀 테스트는 수정 전 토큰 갱신 시 페이지와 MINE 조건이 초기화되는 실패를 확인했다. 별점 UI 검증은 전용 AVD `chaekchaek_state_509`, `emulator-5582`에서 `BookRatingRestorationTest` 3개가 통과했다. 늦은 기존 평점 반영, 사용자 편집 보존, `StateRestorationTester`로 복원한 편집 보존을 확인했다. 이 검증은 주입한 상세 응답과 Compose 저장 상태 복원이며 실제 회원 계정의 서버 요청이나 OS 프로세스 종료 검증이 아니다. 초기 UI 테스트의 Double/Float 비교 오류를 수정한 뒤 세 경우를 다시 실행했다.

보완 후 전체 `:shared:testAndroidHostTest` 233개(실패·오류 0), `:app:assembleDebug`, `:app:lintDebug`, `:shared:compileKotlinIosSimulatorArm64`가 통과했다. 로그는 `/tmp/chaekchaek-an-review-build-final.log`, 별점 UI 테스트 로그는 `/tmp/chaekchaek-an-review-rating-tests.log`다.

인증 복원 대기 표시에는 기존 Loading/Inline progress 컴포넌트(`1058:411`)와 공통 컨트롤 레퍼런스 인스턴스(`1058:413`)를 확인하고 기존 앱 로딩 표시를 재사용했다. 실제 회원 로그인 후 OS 프로세스 복귀·네트워크 장애·쪽수 편집과 iOS UI는 여전히 기기에서 미검증이다.

## 참고

- [Save UI states](https://developer.android.com/topic/libraries/architecture/saving-states)
- [Compose UI 상태 저장](https://developer.android.com/develop/ui/compose/state-saving)
- [Navigation 3 상태 저장](https://developer.android.com/guide/navigation/navigation-3/save-state)
