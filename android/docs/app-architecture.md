# 첵췍 Android 아키텍처

레이어 구조, 패키지 배치, 의존성 주입, 내비게이션, 화면 상태 규칙, 테스트 전략을 정한다.
도메인 객체 자체는 [도메인 모델](domain-model.md), 서버와의 약속은
[API 계약](../../docs/api-contract.md), 화면별 상태·이벤트는 [화면 명세](screen-specs.md)에 있다.

## 1. 전제

| 항목 | 결정 |
| --- | --- |
| 모듈 | 단일 모듈(`:app`) + 패키지로 레이어 분리 |
| DI | Hilt |
| 데이터 공급 | 서버 준비 전까지 인메모리 Fake **DataSource** |
| 내비게이션 | Navigation3 (`nav3Core 1.0.1`) 유지 |
| 테스트 | JUnit5 + Kotest runner + AssertJ + coroutines-test |
| 코드 스타일 | Kotlin official (4칸) |

단일 모듈을 택했지만 나중에 `:domain`을 떼어낼 수 있도록 의존 방향을 지킨다. 그래서
Repository 인터페이스가 `domain`에 있다.

## 2. 레이어와 의존 방향

```
                    ┌──────────────┐
    feature ───────▶│    domain    │◀─────── data
    (Compose, VM)   │ (순수 Kotlin) │   (Retrofit, DataStore, Fake)
                    └──────────────┘
```

`domain`은 아무것도 의존하지 않는다. `feature`와 `data`는 서로를 모르고 둘 다 `domain`만 본다.
Hilt 모듈이 `data`의 구현체를 `domain`의 인터페이스에 묶는 유일한 접점이며, 그 모듈은 `di`
패키지에 있다.

**금지 사항**

- `domain`에서 `android.*`, `androidx.*`, Retrofit, `kotlinx.serialization` 어노테이션 import
- `feature`에서 `data` 패키지 import (Hilt 모듈 제외)
- `data`에서 `feature` 패키지 import

참고 프로젝트는 `feature` 아래 6개 파일이 `data`를 import 한다
(`feature/productlist/ProductListViewModel.kt:18-20` 등). 그 배치를 따르지 않는 이유는
[도메인 모델 부록 A](domain-model.md#부록-a-참고-프로젝트에서-바꾼-것)에 적었다.

## 3. 패키지 구조

```
com.chaekchaek.app
├── domain
│   ├── book        Book, BookId, PageNumber, PageCount, BookRepository
│   ├── shelf       ShelfBook, Shelf, ReadingStatus, ReadingProgress, ShelfRepository
│   ├── note        Note, Quote, Reply, NoteAuthor, NoteSortOrder, NoteScope, NoteRepository
│   ├── spoiler     SpoilerBoundary, NoteVisibility
│   ├── rating      Rating, RatingSummary, RatedBook, RatingRepository
│   ├── reader      ReaderProfile, Nickname, Viewer, GuestQuota, ReaderId, ReaderRepository
│   ├── feed        HomeFeed, FeedSection, FeedRepository
│   └── auth        AuthRepository
├── data
│   ├── remote
│   │   ├── api     Retrofit service 인터페이스
│   │   └── dto     @Serializable DTO + toDomain()
│   ├── datasource  XxxDataSource(interface) + XxxRemoteDataSource
│   ├── fake        XxxFakeDataSource (서버 준비 전)
│   ├── local       DataStore (게스트 쿼터)
│   └── repository  XxxRepositoryImpl
├── di              Hilt 모듈
├── feature
│   ├── home        홈 피드
│   ├── search      검색 (발견 탭)
│   ├── bookdetail  책 상세 + 감상 목록 + 감상 작성 + 별점
│   ├── shelf       내 서재 + 편집 모드
│   ├── root        탭 컨테이너
│   └── common      공용 Composable, UiModel, AppError
├── navigation      NavKey, NavDisplay 구성
└── theme           Color, Type, Theme
```

기존 `ui/search`, `ui/archive`, `data/Book.kt`는 이 구조로 옮긴다. 이동 목록은
[10. 마이그레이션](#10-기존-코드-마이그레이션)에 있다.

## 4. 데이터 레이어

### 4.1 계층

```
domain/note/NoteRepository.kt          interface  (도메인 언어)
        ↑
data/repository/NoteRepositoryImpl.kt  DTO → 도메인 매핑
        ↓
data/datasource/NoteDataSource.kt      interface  (DTO 반환)
        ├── NoteRemoteDataSource.kt    Retrofit 호출
        └── NoteFakeDataSource.kt      더미 DTO 반환
```

**DataSource는 DTO를 반환하고 Repository가 도메인으로 바꾼다.** 참고 프로젝트는 DataSource가
도메인을 반환해서 Repository가 한 줄 위임만 하는 빈 계층이 되었다
(`data/repository/product/ProductRepositoryImpl.kt`). 같은 실수를 하지 않는다.

인터페이스는 도메인 언어로 쓴다. 서버 쿼리 파라미터 모양을 그대로 노출하지 않는다.

```kotlin
// domain/note/NoteRepository.kt
interface NoteRepository {
    suspend fun notes(
        bookId: BookId,
        sort: NoteSortOrder,
        scope: NoteScope,
    ): List<Note>

    suspend fun write(bookId: BookId, draft: NoteDraft): Note

    suspend fun like(noteId: NoteId): Note

    suspend fun reply(noteId: NoteId, content: String): Reply
}

enum class NoteSortOrder { LATEST, POPULAR }   // Figma: 최신순 / 인기순
enum class NoteScope { ALL, MINE }             // Figma: 전체 피드 / 내 피드
```

참고 프로젝트의 `loadProducts(page, pageSize, sort: List<String>, category: String?)`처럼
`List<String>` 정렬 키를 도메인 인터페이스에 노출하지 않는다.

### 4.2 Fake

Fake는 **DataSource 레벨**에 둔다. Repository 레벨에 두면 매핑 코드가 서버 붙는 날까지 한 번도
실행되지 않는다. DataSource에 두면 개발 내내 `dto.toDomain()`이 돌아 매핑 버그와 DTO 설계 문제를
미리 발견한다.

```kotlin
// data/fake/NoteFakeDataSource.kt
class NoteFakeDataSource @Inject constructor() : NoteDataSource {
    override suspend fun notes(bookId: String, sort: String, scope: String): List<NoteDto> {
        delay(FAKE_DELAY_MILLIS)   // 로딩 상태를 실제로 보기 위해
        return FakeNotes.of(bookId)
    }
}
```

더미 표지 이미지는 저장소의 `images/`를 쓴다(이슈 #7의 요구). Fake가 반환하는 DTO는
[API 계약](../../docs/api-contract.md)의 예시 응답과 **같은 모양**이어야 한다. 계약이 바뀌면 Fake도
같이 바꾼다.

**검증되지 않는 것**: JSON 파싱, HTTP 오류 코드, 페이징, 인증 헤더. 서버가 붙는 시점에 처음
마주치는 영역이라는 것을 인지하고 간다.

### 4.3 서버 전환

Hilt 모듈에서 바인딩 한 줄을 바꾸면 된다.

```kotlin
// di/DataSourceModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    abstract fun bindNoteDataSource(impl: NoteFakeDataSource): NoteDataSource
    //                                   ^^^^^^^^^^^^^^^^^^ 서버 준비 시 NoteRemoteDataSource
}
```

## 5. 의존성 주입 (Hilt)

### 5.1 추가할 설정

```kotlin
// gradle/libs.versions.toml
hilt = "2.5x"          // 착수 시점 최신 안정 버전 확인
ksp  = "2.3.20-x.x.x"  // Kotlin 2.3.20에 맞는 KSP 버전 확인

// app/build.gradle.kts
plugins {
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}
```

정확한 버전은 미확인이다. 착수 시 Hilt와 KSP가 Kotlin 2.3.20·AGP 9.0.1과 호환되는 조합인지
확인하고 고정한다.

### 5.2 구성

```kotlin
@HiltAndroidApp
class ChaekchaekApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity()

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val noteRepository: NoteRepository,
    private val shelfRepository: ShelfRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel()
```

모듈은 세 개로 나눈다.

| 모듈 | 내용 |
| --- | --- |
| `DataSourceModule` | DataSource 인터페이스 ↔ Fake/Remote 바인딩 (`@Binds`) |
| `RepositoryModule` | Repository 인터페이스 ↔ Impl 바인딩 (`@Binds`) |
| `NetworkModule` | Retrofit, OkHttp, Json 제공 (`@Provides`). 서버 붙을 때 채운다 |

참고 프로젝트는 `ShoppingApplication.initDependencies()`에서 손으로 그래프를 조립한다
(약 70줄). Hilt를 쓰면 그 코드가 사라지는 대신 KSP 빌드 시간이 붙는다.

## 6. 화면 레이어

### 6.1 UiState

기본은 **sealed 3상태**다.

```kotlin
sealed interface ShelfUiState {
    data object Loading : ShelfUiState

    data class Failure(val error: AppError) : ShelfUiState

    data class Content(
        val books: List<ShelfBookUiModel>,
        val filter: ShelfFilterUiModel,
        val editing: ShelfEditUiModel?,
    ) : ShelfUiState
}
```

**부분 갱신이 실제로 있는 화면만** `Content` 안에 하위 상태를 중첩한다. 책 상세가 그렇다.
감상 정렬(최신순/인기순)이나 범위(전체 피드/내 피드)를 바꾸면 감상 목록만 다시 불러오고,
표지·별점·내 독서 기록은 그대로 있어야 한다.

```kotlin
sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState

    data class Failure(val error: AppError) : BookDetailUiState

    data class Content(
        val book: BookUiModel,
        val myRecord: ReadingRecordUiModel,
        val notes: NoteListUiState,          // 중첩
        val dialog: BookDetailDialog?,       // 스포일러/별점 다이얼로그
    ) : BookDetailUiState
}

sealed interface NoteListUiState {
    data object Loading : NoteListUiState
    data class Failure(val error: AppError) : NoteListUiState
    data class Loaded(
        val notes: List<NoteUiModel>,
        val sort: NoteSortOrder,
        val scope: NoteScope,
        val totalCount: Int,
    ) : NoteListUiState
}
```

참고 프로젝트의 `ProductListUiState`는 `isLoading`·`error`·데이터를 한 data class에 담아
`isLoading = true && error != null && 데이터 있음` 같은 조합이 타입상 가능하다. 그 상태를
Composable이 매번 해석해야 한다. 우리는 그 조합을 만들 수 없게 한다.

### 6.2 UiModel

**ViewModel이 도메인을 UiModel로 바꾼다.** Composable은 도메인 타입을 모른다.

이유가 둘이다. 첫째, 가려진 감상은 **본문을 아예 담지 않고** 만들 수 있다. 화면에 도달하지 않은
문자열은 실수로도 그릴 수 없다. 둘째, `book.totalPages.value`를 꺼내 `"308쪽"`을 조립하는 코드가
Composable마다 반복되지 않는다.

```kotlin
sealed interface NoteUiModel {
    val id: NoteId
    val authorLabel: String       // "참새 1204 (익명)" 또는 "골똘한 참새"

    data class Visible(
        override val id: NoteId,
        override val authorLabel: String,
        val dateLabel: String,        // "2026.08.05"
        val readingPointLabel: String,// "p.80까지"
        val completedBadge: Boolean,  // "완독"
        val impression: String,
        val quote: QuoteUiModel?,
        val likeLabel: String,        // "좋아요 12"
        val likedByMe: Boolean,
        val replyLabel: String,       // "답글 2"
        val replies: List<ReplyUiModel>,
    ) : NoteUiModel

    data class Hidden(
        override val id: NoteId,
        override val authorLabel: String,
        val dateLabel: String,
        val requiredPageLabel: String,  // "160쪽"
    ) : NoteUiModel                     // 본문·인용문 없음
}
```

`Hidden`에는 `impression`도 `quote`도 없다. 스포일러 유출이 타입 수준에서 불가능해진다.

UiModel에서는 **표시용 문자열까지 완성한다.** 참고 프로젝트의 `ProductUiModel(price: Int)`처럼
값을 원시 타입으로 풀어서 화면이 다시 계산하게 두지 않는다.

### 6.3 ViewModel

```kotlin
@HiltViewModel
class ShelfViewModel @Inject constructor(
    private val shelfRepository: ShelfRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ShelfUiState>(ShelfUiState.Loading)
    val uiState: StateFlow<ShelfUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<UiEvent>()
    val event: SharedFlow<UiEvent> = _event.asSharedFlow()
}
```

- 지속 상태는 `StateFlow<XxxUiState>`, 일회성은 `SharedFlow<UiEvent>`
- 도메인 객체는 ViewModel 안에서만 다루고 밖으로 내보내지 않는다
- 화면 전환은 ViewModel이 하지 않는다. Composable이 콜백으로 받는다

### 6.4 Composable

- `NavController`/`NavBackStack`을 받지 않는다. `onBookClick: (BookId) -> Unit` 같은 콜백을 받는다
- 상태는 `collectAsStateWithLifecycle()`로 받는다
- 화면 Composable(상태 연결)과 내용 Composable(상태 없음)을 나눈다. 내용 쪽에 `@Preview`를 붙인다

```kotlin
@Composable
fun ShelfScreen(
    onBookClick: (BookId) -> Unit,
    viewModel: ShelfViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShelfContent(uiState = uiState, onBookClick = onBookClick, ...)
}

@Composable
private fun ShelfContent(uiState: ShelfUiState, ...) { ... }
```

## 7. 내비게이션

### 7.1 구조

Figma에서 **책 상세·서재 편집·검색 결과에는 하단 탭바가 없고**, 홈과 내 서재에만 있다. 상세가
탭 위로 올라오는 전체 화면이라는 뜻이다. 그래서 탭은 백스택에 넣지 않는다.

```
backStack
  [RootKey]                                    탭 컨테이너 (탭바 보임)
  [RootKey, BookDetailKey(bookId)]             책 상세 (탭바 없음)
  [RootKey, BookDetailKey, NoteComposeKey]     감상 작성
  [RootKey, ShelfEditKey]                      서재 편집
```

`RootKey` 내부에서 탭을 고른다.

```kotlin
enum class RootTab { HOME, DISCOVER, SHELF }   // Figma: 홈 / 발견 / 내 서재
```

「발견」 탭은 검색 화면(36:427)을 쓴다. 시안에 탭바가 없으므로 **하단에 탭바를 추가**해야 하고,
기존 우하단 원형 버튼과 겹치지 않게 위치를 조정해야 한다.

**감수한 한계**: 탭별 독립 백스택이 없다. 홈에서 상세를 열고 다른 탭에 갔다 돌아오면 상세가
아니라 탭 목록이 보인다. 필요해지면 그때 탭별 백스택을 도입한다.

### 7.2 NavKey

```kotlin
@Serializable data object RootKey : NavKey
@Serializable data class BookDetailKey(val bookId: String) : NavKey
@Serializable data class NoteComposeKey(val bookId: String) : NavKey
@Serializable data object ShelfEditKey : NavKey
```

`NavKey`는 직렬화되어야 하므로 `BookId` 대신 `String`을 담는다. 화면 진입 직후 `BookId`로 감싼다.

Navigation3 1.0.1의 세부 API(탭별 백스택, 전환 애니메이션)는 미확인이다. 구현 착수 시 공식
문서를 확인해 맞춘다.

## 8. 오류 처리

```kotlin
sealed interface AppError {
    data object Network : AppError
    data object NotFound : AppError
    data object Unauthorized : AppError
    data object Unknown : AppError
}

fun Throwable.toAppError(): AppError = when (this) {
    is IOException -> AppError.Network
    is HttpException -> when (code()) {
        404 -> AppError.NotFound
        401, 403 -> AppError.Unauthorized
        else -> AppError.Unknown
    }
    else -> AppError.Unknown
}
```

**표시 방식은 다이얼로그다.** Figma에 오류 시안이 없어 새로 만든다. 확인 버튼과 재시도 버튼을
두고, 재시도가 의미 없는 오류(`NotFound`)는 확인만 둔다.

부분 실패(감상 목록만 실패)는 화면 전체가 아니라 해당 영역의 상태를 `Failure`로 만든다.

## 9. 테스트

### 9.1 도구

```kotlin
testImplementation(libs.junit.jupiter)
testImplementation(libs.kotest.runner.junit5)
testImplementation(libs.assertj.core)
testImplementation(libs.kotlinx.coroutines.test)
```

기존 `BookTest.kt`는 JUnit4라 JUnit5로 옮긴다.

### 9.2 무엇을 테스트하나

| 대상 | 방법 | 우선순위 |
| --- | --- | --- |
| 도메인 불변식·상태 전이 | 순수 JUnit5. [규칙 목록 30개](domain-model.md#10-규칙-목록-테스트-대상)를 그대로 옮긴다 | 높음 |
| 스포일러 판정 | 경계값 위주 (`readingPoint` == 읽은 쪽수, ±1) | 높음 |
| ViewModel | Fake Repository + `runTest` + `MainDispatcher` 교체 | 중간 |
| DTO 매핑 | `toDomain()` 단위 테스트 | 중간 |
| Composable | `ui-test-junit4`로 핵심 흐름만 | 낮음 |

ViewModel 테스트용 Fake는 `src/test`에 둔다. `src/main`의 `data/fake`(개발용 더미)와 목적이
다르므로 공유하지 않는다. 개발용 Fake는 "그럴듯한 화면"을 만들고, 테스트용 Fake는 "특정 조건"을
만든다.

### 9.3 스타일

참고 프로젝트를 따라 한글 백틱 함수명과 given/when/then 주석을 쓴다.

```kotlin
@Test
fun `다 읽음으로 바꾸면 진행 쪽수가 총 쪽수가 된다`() {
    // given : 308쪽 중 80쪽을 읽은 책이 서재에 있다
    val shelfBook = shelfBook(current = 80, total = 308)

    // when : 상태를 다 읽음으로 바꾸면
    val finished = shelfBook.changeStatus(ReadingStatus.FINISHED, at = FIXED_INSTANT)

    // then : 진행 쪽수가 308쪽이 된다
    assertThat(finished.progress.currentPage).isEqualTo(PageNumber(308))
}
```

시각이 들어가는 객체는 `Instant`를 인자로 받게 만들어 테스트에서 고정값을 넣는다.
`Instant.now()`를 도메인 안에서 부르지 않는다.

## 10. 기존 코드 마이그레이션

현재 `app/src/main`은 811줄이고 레이어가 없다. 아래처럼 옮긴다.

| 현재 | 이동 후 | 비고 |
| --- | --- | --- |
| `data/Book.kt` | `domain/book/Book.kt` + `data/remote/dto/BookDto.kt` | 모델과 파싱을 분리 |
| `data/BookSearchApi.kt` | `data/remote/api/BookApi.kt` | `object` 싱글턴 → 주입 가능한 클래스 |
| `data/ArchiveRepository.kt` | `data/repository/ShelfRepositoryImpl.kt` | `Context` 직접 사용 제거 |
| `ui/search/*` | `feature/search/*` | |
| `ui/archive/*` | `feature/shelf/*` | |
| `RootScreen.kt` | `feature/root/RootScreen.kt` | 탭 2개 → 3개 |
| `Navigation.kt`, `NavigationKeys.kt` | `navigation/*` | |
| `theme/*` | 그대로 | |

현재 코드에서 함께 고칠 문제들이다.

1. `BookSearchApi`가 `object` 싱글턴이라 테스트에서 바꿔 끼울 수 없고 `BuildConfig`를 직접
   참조한다. 클래스로 바꿔 주입한다.
2. `ArchiveRepository(context)`가 `RootScreen`의 `remember { }`에서 생성된다
   (`RootScreen.kt:32`). 화면이 의존성을 만들면 생명주기와 테스트가 꼬인다. Hilt로 옮긴다.
3. `BookSearchApi.search`의 `catch (e: IOException) { fetch(url) }`은 재시도 1회를 예외 처리로
   표현해 의도가 드러나지 않는다. 명시적 재시도로 바꾸거나 없앤다.
4. `data/Book.kt`에 모델·JSON 파싱·직렬화가 한 파일에 섞여 있다.

## 11. 빌드 설정 변경 목록

1. Hilt + KSP 플러그인·의존성 추가 (버전 호환 확인 필요)
2. 테스트 의존성 교체: JUnit4 → JUnit5 + Kotest runner + AssertJ, `useJUnitPlatform()` 설정
3. `.editorconfig` 추가 (`indent_size = 4`). `gradle.properties`의 `kotlin.code.style=official`과
   실제 코드(2칸)가 어긋난 상태를 해소한다
4. 기존 `.kt` 15개 재포맷 (별도 커밋으로 분리해 리뷰가 섞이지 않게 한다)
5. `local.properties`에 API base URL 항목 추가 (서버 붙을 때)

## 12. 구현 순서 (이슈 분할안)

프로젝트 규칙상 이슈는 "사용자가 완료된 동작 하나를 확인할 수 있는 단위"다. 아래는 제안이며
확정 전에 함께 조정한다.

| 순서 | 이슈 | 확인 가능한 동작 |
| --- | --- | --- |
| 0 | 빌드 기반 정비 | Hilt·테스트 도구·포맷 설정이 들어가고 기존 앱이 그대로 동작한다 |
| 1 | 도메인 모델 + 테스트 | 규칙 30개가 테스트로 통과한다 (화면 변화 없음) |
| 2 | 탭 3개 + 홈 피드 (이슈 #7) | 앱을 켜면 홈 피드가 보이고 탭 3개로 이동한다 |
| 3 | 검색(발견 탭) | 책을 검색하고 「읽는 중 시작」으로 서재에 담는다 |
| 4 | 내 서재 목록 | 상태 필터와 정렬로 서재를 본다 |
| 5 | 책 상세 + 내 독서 기록 | 상태를 바꾸고 쪽수를 기록한다 |
| 6 | 감상 목록 + 스포일러 가드 | 감상을 읽고, 안 읽은 구간은 가려진다 |
| 7 | 감상 작성 | 감상을 남기면 목록에 나타난다 |
| 8 | 답글 + 좋아요 | 감상에 답글을 달고 좋아요를 누른다 |
| 9 | 별점 | 별점을 매기고 최근 기록을 본다 |
| 10 | 서재 편집 모드 | 여러 권을 골라 상태를 바꾸거나 지운다 |
| 11 | 닉네임·익명 설정 | 익명 공개를 끄고 닉네임을 정한다 |
| 12 | 게스트 열람 제한 | 감상 3개를 보면 배너가 소진 상태가 된다 |

0과 1은 화면 변화가 없어 이슈 단위 규칙에서 벗어난다. 다른 작업의 선행 조건이라 예외로 두거나,
2에 합쳐도 된다.
