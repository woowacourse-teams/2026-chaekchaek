# Android 3개 화면 상태 보존 구현 핸드오프

## 목적과 현재 상태

스프린트 요구사항: 최소 3개 화면의 상태를 ViewModel, Saved State, Persistent Storage 중 알맞은 곳에 둔다.

대상은 발견, 내 서재, 피드다. 세 저장 방식을 화면마다 하나씩 배정하는 요구사항으로 해석하지 않는다. 같은 화면에서도 데이터의 수명과 복원 목적에 따라 저장 위치를 나눈다.

- 작성일: 2026-10-14.
- 조사 기준: `origin/an-develop`의 `fb11f58e8eccbfdda3da7d52b00fd4e4d38ace3d`.
- 이 문서는 앞선 적용 제안을 구현 담당자에게 전달하는 계획이다. 앱 코드 수정, 자동 테스트, 기기에서의 상태 복구 검증은 아직 수행하지 않았다.
- 별도 요구사항인 debug 전용 개발자 도구는 이 문서의 구현 범위에 포함하지 않는다.
- 코드 변경 범위는 `/android`다. 백엔드 API 추가, 웹 변경, 스토어 배포는 포함하지 않는다.

## 다음 세션에 전달할 요청

> docs/android-state-preservation-handoff.md를 읽고 발견, 내 서재, 피드의 상태 보존을 구현해 주세요. 먼저 최신 an-develop과 현재 변경 사항을 확인하고 요구사항을 재진술해 주세요. intent-first와 flow-first를 적용하고, 아래 저장 정책과 왕복 흐름을 기준으로 ViewModel 소유권, 탭 상태 보존, 프로세스 복원을 연결해 주세요. 결과가 달라지는 미확정 동작은 구현 전에 질문해 주세요. 이슈와 Draft PR은 각각 제목·본문 초안을 제시하고 AGENTS.md의 별도 슛 승인을 받은 뒤 생성하세요. 세 화면의 회전, 탭 왕복, 상세 왕복, 시스템 프로세스 종료 복구를 검증하고 자동 테스트와 기기 검증의 결과를 구분해 기록해 주세요.

## 현재 코드에서 확인한 내용

아래 경로는 저장소 루트 기준이다. 후속 작업 시 최신 코드와 다시 비교한다.

| 파일 | 확인한 내용 | 수정 방향 |
| --- | --- | --- |
| [Navigation.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/Navigation.kt) | SearchViewModel, ArchiveViewModel, FeedViewModel 등을 `remember { ... }`로 직접 생성한다. `rememberNavBackStack`으로 Root와 상세 등의 경로는 관리한다. | ViewModelStoreOwner와 factory를 통해 대상 ViewModel의 수명을 명시한다. 백스택 저장과 ViewModel 저장을 별도로 연결한다. |
| [RootScreen.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/RootScreen.kt) | 선택 탭은 `rememberSaveable`이지만 탭 콘텐츠는 `when(selectedTab)`으로 교체한다. 탭별 SaveableStateHolder는 없다. | Root가 살아 있는 동안 탭별 UI 상태를 보관한다. |
| [SearchViewModel.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/search/SearchViewModel.kt) | 입력값 `_query`, 실행한 검색어 `currentQuery`, 정렬 `_sort`, 결과 `_uiState`가 메모리에 있다. 최근 검색어는 저장소를 사용한다. | 입력 중인 값과 실행한 검색 조건을 구분해 복원한다. |
| [SearchScreen.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/search/SearchScreen.kt) | `leaveSearch`가 `onClear()` 후 뒤로 이동한다. | 사용자가 검색을 종료하는 동작과 탭 전환·일시 중단을 구분한다. |
| [ArchiveScreen.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/archive/ArchiveScreen.kt) | 필터, 정렬, 책등 표시 옵션은 화면 내부 `rememberSaveable`, 목록 위치는 `rememberLazyListState`다. 선택 ID와 삭제 확인 대상은 `remember`다. | 기존 저장 가능한 값을 탭 수명에 연결한다. 편집·삭제 동작을 자동 재실행하지 않는다. |
| [FeedScreen.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/feed/FeedScreen.kt) | 펼친 스포일러 ID는 `remember`로 관리한다. 목록 state를 화면 상위에서 명시적으로 전달하지 않는다. | 스포일러 ID와 목록 위치를 탭별로 보존한다. |
| [RecentSearchPreferences.kt](../android/app/src/main/java/com/chamsae/chaekchaek/search/RecentSearchPreferences.kt) | SharedPreferences에 최근 검색어를 저장한다. | 기존 Persistent Storage 구현을 재사용한다. 새 DB나 저장 라이브러리 도입은 요구하지 않는다. |

## 저장 위치와 보존 범위

| 화면 | ViewModel | Saved State | Persistent Storage |
| --- | --- | --- | --- |
| 발견 | 검색 결과, 페이지 정보, 진행 중인 검색 작업 | 입력 중인 검색어, 마지막 실행 검색어, 정렬, 목록 복원 위치 | 기존 최근 검색어 목록 |
| 내 서재 | 서버에서 받은 서재 목록, 조회 상태 | 독서 상태 필터, 정렬, 책등 표시 옵션, 목록 복원 위치 | 이번 작업에서 추가하지 않음 |
| 피드 | 피드 목록, 페이지 정보, 조회 상태 | 펼친 스포일러 ID, 목록 복원 위치 | 이번 작업에서 추가하지 않음 |

- ViewModel은 화면 회전과 해당 소유 범위 내 화면 왕복 동안 데이터를 유지한다. 프로세스 종료 후 같은 객체가 되살아나는 것으로 설명하지 않는다.
- Saved State에는 작고 직렬화 가능한 복원 단서만 둔다. 결과 목록, 이미지, 전체 응답 객체, 인증 토큰을 저장하지 않는다.
- 프로세스 복구 시 서버 데이터는 다시 조회한다. 재조회한 데이터에 저장 조건과 목록 위치를 적용한다.
- 앱을 명시적으로 종료한 뒤 새 작업으로 실행할 때까지 보장하는 값은 이번 범위에서는 최근 검색어다. Saved State를 영구 저장으로 취급하지 않는다.
- 순수 표시 상태는 `rememberSaveable`과 Saver로 관리할 수 있다. ViewModel의 조회·필터링 로직이 소유하는 값은 `SavedStateHandle`로 관리한다. 같은 값을 두 곳에 독립적으로 저장하지 않는다.

## 구현 순서와 주의점

### 1. ViewModel의 소유 범위부터 연결

- 발견, 내 서재, 피드는 같은 Root 목적지 안의 탭이다. Root의 ViewModelStoreOwner에서 화면별 ViewModel을 가져오는 구성을 우선 검토한다.
- Navigation 3의 entry별 ViewModel 소유권과 저장 상태 연결을 설정한 뒤 `viewModel()`과 factory를 사용한다. 대상 ViewModel 생성이 아직 AppNavigation의 상위에 있다면 소유 범위를 실제로 전달하거나 생성 위치를 조정해야 한다.
- Shared 모듈은 KMP다. [shared/build.gradle.kts](../android/shared/build.gradle.kts)와 [libs.versions.toml](../android/gradle/libs.versions.toml)에서 실제 연결된 의존성과 commonMain 지원 여부를 확인한다. 버전 카탈로그에 항목이 존재하는 것만으로 의존성 연결 완료로 판단하지 않는다.
- `SavedStateHandle()`을 수동으로 새로 만드는 것만으로 복원 연결을 대신하지 않는다. owner의 저장 상태와 연결되는 생성 경로를 사용한다.
- SearchViewModel의 `registerBook`, `isSignedIn`은 현재 다른 ViewModel을 캡처한다. FeedViewModel의 review actions도 플랫폼 인증 콜백을 참조한다. 수명을 늘린 객체가 회전 전 AuthViewModel, 등록 ViewModel, Activity 또는 취소된 coroutine scope를 계속 참조하지 않는지 확인한다.
- 연관 객체의 소유권 조정은 이 문제 해결에 필요한 범위에서 한다. 인증 세션 복구 중 일시적인 미인증 상태가 보존한 목록을 불필요하게 초기화하지 않도록 확인하되, 실제 로그아웃·계정 변경은 기존 초기화 동작을 유지한다.
- Root 소멸 시 정리와 상세 화면 왕복 시 유지를 구분한다. Composable이 잠깐 화면에서 빠졌다는 이유만으로 ViewModel을 수동 종료하지 않는다.

### 2. 탭별 UI 상태 보존

- Root에 `rememberSaveableStateHolder()`를 두고 탭의 안정적인 문자열 키로 `SaveableStateProvider`를 적용한다. 조건 분기 바깥에서 holder를 유지한다.
- 내 서재의 기존 `rememberSaveable` 및 `rememberLazyListState`가 이 범위 안에 있도록 배치한다.
- 발견과 피드도 목록 state를 명시적으로 소유하고 실제 LazyColumn에 전달한다. 로딩·빈 화면 분기로 목록이 잠깐 제거될 때 저장 위치가 유실되지 않도록 한다.
- 피드의 펼친 스포일러 ID는 저장 가능한 기본형 목록과 Saver 등으로 복원한다. 삭제되거나 접근할 수 없는 콘텐츠는 복원 결과에서 제외한다.
- 내 서재 동일 탭 재선택 시 맨 위로 이동하는 기존 동작은 유지한다. 일반 탭 왕복과 명시적인 맨 위 이동을 구분한다.
- 서재 편집 상태는 Root에 있고 선택 ID는 화면에 있다. 한쪽만 보존해 선택이 사라진 편집 화면이 되지 않도록 기존 탭 전환의 편집 종료 정책과 회전 복원 정책을 함께 확인한다. 회전 시 선택 유지 여부 등 기존 제안에서 확정하지 않은 편집 정책은 구현 전에 질문한다.

### 3. 검색과 목록 복원

- 발견에서 A로 검색 후 입력란만 B로 수정했다면, 복구 후 입력란은 B이고 조회 결과는 A의 조건이어야 한다. 실행한 검색이 없으면 입력만 복원하고 자동 검색하지 않는다.
- 복원 조회는 사용자 검색 제출과 구분한다. 최근 검색어를 재추가하거나 사용자 제출 이벤트를 중복 기록하지 않는다. 현재 `search()`의 `submit` 및 `sort_change` 경로를 그대로 재사용할 때의 부수효과를 확인한다.
- 최신 기준에는 `SearchInputRules`가 있다. 복원 문자열에도 같은 100 code point 제한을 적용한다. 기존 길이 제한을 문자 수가 다른 임의 구현으로 대체하지 않는다.
- `clear()`는 복원할 검색어와 실행 조건도 지워야 한다. 종료한 검색이 다음 복원 때 다시 나타나면 안 된다. 정렬 초기화 여부는 현재 동작을 유지한다.
- 시스템 프로세스 종료 후 검색·피드 목록을 복구할 때 저장된 `nextPage`만 가져와 중간 페이지부터 시작하지 않는다. 첫 페이지부터 필요한 구간을 복구하고 중복 항목을 제거한다.
- 목록 위치 복원에는 가능하면 안정적인 콘텐츠 ID와 offset을 사용한다. 정렬 변화·항목 삭제로 대상이 없어진 경우의 대체 위치, 복원 조회의 최대 범위와 중단 조건은 구현 전에 정한다. 이 세부 정책은 아직 미확정이다.
- 데이터를 다 받기 전에 빈 목록에 스크롤을 적용하거나 기본 위치로 저장값을 덮어쓰지 않는다. 복원 실패 시 반복 조회에 빠지지 않고 기존 오류·재시도 흐름을 사용한다.
- 계정 변경, 필터 변경, 새 검색처럼 데이터 집합이 달라지는 동작에는 이전 집합의 복원 위치를 적용하지 않는다.
- 좋아요, 등록, 삭제, 감상·답글 제출은 상태 복원 때문에 재실행하지 않는다.

### 4. 기존 이동과 UI 유지

- 최신 an-develop의 [NavigationBackStack.kt](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/NavigationBackStack.kt)에 있는 루트 보호와 현재 목적지 확인을 유지한다. 과거 체크아웃의 무조건적인 `removeLastOrNull()`로 되돌리지 않는다.
- 새 화면이나 시각 디자인 변경은 이번 작업에 필요하지 않다. 상태 복구를 위해 UI 변경이 필요해지면 해결할 문제와 변경할 UI를 먼저 설명하고 저장소의 Figma 확인 절차를 따른다.
- Android 상태 보존이 완료 기준이다. commonMain 변경은 iOS에도 영향을 주므로 Android 전용 API를 공통 코드에 직접 넣지 않고 iOS 컴파일 회귀도 확인한다. iOS 기기에서 검증하지 않았다면 미검증으로 남긴다.

## 검증과 완료 기준

| 시나리오 | 기대 결과 |
| --- | --- |
| 발견: 검색, 정렬, 추가 페이지 조회 후 회전 | 입력·실행 조건·결과·읽던 위치 유지. 사용자 제출 이벤트와 최근 검색어 기록 중복 없음 |
| 발견: A 검색 후 B 입력만 하고 시스템 프로세스 종료·복구 | B 입력과 A 결과를 구분해 복원. 미실행 검색 자동 제출 없음 |
| 발견: 탭 왕복 및 책 상세 왕복 | 검색 조건과 읽던 위치 유지 |
| 발견: 기존 뒤로가기로 검색 종료 후 복원 | 종료한 검색이 재등장하지 않음 |
| 내 서재: 필터·정렬·책등 옵션 변경 후 회전, 탭·상세 왕복 | 선택값과 읽던 위치 유지. 동일 탭 재선택의 맨 위 이동은 정상 |
| 피드: 스포일러 펼치기, 추가 페이지 조회 후 회전, 탭·상세 왕복 | 목록·읽던 위치·펼친 스포일러 유지 |
| 세 화면: 백그라운드에서 시스템 프로세스 종료 후 기존 작업 복귀 | 조건 복원, 데이터 재조회, 확보된 목록에 위치 적용 |
| 복원 중 네트워크 실패, 대상 항목 삭제 | 무한 조회·잘못된 위치 적용 없이 합의한 실패·대체 동작 수행 |
| 회전 후 등록·좋아요 등 기존 기능 사용, 로그아웃·계정 변경 | 오래된 콜백을 사용하지 않으며 다른 계정의 데이터·위치를 보여주지 않음 |
| 앱을 명시적으로 종료한 뒤 새로 실행 | 최근 검색어 유지. 다른 임시 상태의 영구 보존은 요구하지 않음 |

자동 검증과 기기 검증을 따로 기록한다.

- ViewModel 테스트: 저장 조건에서 재생성, 미실행 입력 구분, clear 후 복원 방지, 복원 시 기록 부수효과 방지, 페이지 복구 실패·취소를 검증한다.
- Compose 테스트: 탭별 SaveableStateHolder와 Saver 복원, 실제 목록 state 연결을 검증한다. 필요하면 `StateRestorationTester`를 사용한다.
- Android 통합 검증: Activity 재생성과 화면 왕복을 수행한다. `ActivityScenario.recreate()`는 프로세스 종료 테스트를 대신하지 않는다.
- 프로세스 종료 검증: 앱을 백그라운드로 보낸 뒤 프로세스를 종료하고 최근 앱의 기존 작업으로 복귀한다. 강제 종료나 최근 앱에서 작업 삭제는 Saved State 복구 증거로 사용하지 않는다. PID 변경, 조작 순서, 복구 결과를 남긴다.
- Gradle 검증은 현재 등록된 task를 확인한 후 shared host 테스트, app 단위 테스트, debug 빌드와 lint를 실행한다. 현재 CI는 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`를 실행하므로 shared 복원 테스트가 실제 실행됐는지 별도로 확인한다.
- 완료 보고에는 검증한 커밋, Android 기기·OS, 세 화면별 시나리오, 통과·실패·미검증, 관련 테스트나 캡처 경로를 적는다. ViewModel 클래스 존재나 빌드 성공만으로 요구사항 완료를 판단하지 않는다.

## 참고 문서

- [Compose UI 상태 저장](https://developer.android.com/develop/ui/compose/state-saving): ViewModel, SavedStateHandle, rememberSaveable의 역할과 저장 크기 제한.
- [Navigation 3 상태 관리](https://developer.android.com/guide/navigation/navigation-3/save-state): 백스택과 entry별 ViewModel 소유 범위.
- [SaveableStateHolder](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/SaveableStateHolder): Composition에서 제거된 하위 UI의 저장 가능한 상태 보관.
- [Android UI 상태 저장](https://developer.android.com/topic/libraries/architecture/saving-states): 시스템 종료와 사용자 종료, 저장 시점의 차이.
