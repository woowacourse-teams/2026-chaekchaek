# 첵췍 앱 아키텍처 (KMP)

Android(Compose)와 iOS(SwiftUI) 두 앱을 Kotlin Multiplatform으로 만든다. 레이어 구조, 모듈 경계,
의존성 주입, 화면 상태 규칙, 테스트 전략을 정한다.

- 도메인 객체: [도메인 모델](domain-model.md)
- 모듈 구성·빌드·iOS 연동: [KMP 셋업](kmp-setup.md)
- 서버와의 약속: [API 계약](../../docs/api-contract.md)
- 화면별 상태·액션: [화면 명세](screen-specs.md)

## 1. 전제

| 항목 | 결정 |
| --- | --- |
| 플랫폼 | Android + iOS. 공유 모듈 하나 |
| 공유 범위 | domain + data + **presentation** (ViewModel·UiModel까지) |
| UI | Android는 Compose, iOS는 SwiftUI. 각자 작성 |
| DI | kotlin-inject (컴파일타임 검증, KMP 지원) |
| 네트워크 | Ktor Client |
| 시각 | kotlinx-datetime |
| 직렬화 | kotlinx.serialization |
| ViewModel | `androidx.lifecycle.ViewModel` (2.10.0+, KMP 지원) |
| 데이터 공급 | 서버 준비 전까지 인메모리 Fake DataSource (commonMain) |
| 표시 문자열 | commonMain에 한국어 직접 |
| Swift 브리지 | iOS 착수 시 결정 (SKIE 유력) |
| 테스트 | kotlin.test + Kotest assertions + coroutines-test |
| 코드 스타일 | Kotlin official (4칸) |

Hilt·Retrofit·`java.time`은 KMP에서 쓸 수 없어 제외했다. 자세한 근거는
[부록 B](#부록-b-kmp-때문에-바뀐-결정)에 있다.

## 2. 공유 범위를 presentation까지 넓힌 이유

두 가지 문제를 동시에 푼다.

**첫째, 스포일러 안전장치가 한 곳에 남는다.** 이 앱의 핵심 규칙은 `isSpoiler`가 설정된 감상을
가리고, 사용자가 탭한 `reviewId` 한 건만 공개하는 것이다. presentation을 공유하면 Android와
iOS가 같은 판정과 공개 상태를 사용한다.

**둘째, `value class`가 Swift 경계에 노출되지 않는다.** Kotlin/Native의 Objective-C export는
inline/value class를 underlying 타입이나 `id`(Swift의 `Any`)로 매핑한다. 즉 `PageNumber`가
Swift에서 `Int32`나 `Any`로 보여 타입 안전성이 사라진다. UiModel이 `"80쪽 / 308쪽"` 같은 완성된
`String`만 담으면 `value class`가 Swift까지 나가지 않는다.

**대가**: iOS 화면 구조가 Android를 따라가고 SwiftUI다운 문법이 줄어든다. UiModel 형태를 바꿀 때
양쪽 UI가 함께 영향을 받는다.

## 3. 레이어와 의존 방향

```
       :androidApp (Compose)          :iosApp (SwiftUI)
              │                              │
              └──────────────┬───────────────┘
                             ▼
                    :shared / presentation
                    (ViewModel, UiState, UiModel)
                             │
                             ▼
                    :shared / domain
                    (순수 Kotlin, 인터페이스)
                             ▲
                             │
                    :shared / data
                    (Ktor, DataStore, Fake, Impl)
```

- `domain`은 아무것도 의존하지 않는다
- `presentation`과 `data`는 서로를 모르고 둘 다 `domain`만 본다
- `androidApp`·`iosApp`은 `presentation`만 본다. `data`를 직접 보지 않는다
- DI 그래프가 `data`의 구현체를 `domain`의 인터페이스에 묶는 유일한 접점이다

**금지 사항**

- `domain`에서 `androidx.*`, Ktor, `kotlinx.serialization` 어노테이션 import
- `presentation`에서 `data` 패키지 import (DI 컴포넌트 제외)
- `commonMain` 어디서든 `java.*` 사용 (Kotlin/Native에 없다)
- `androidApp`에서 `domain`·`data` 타입 직접 사용

## 4. 패키지 구조

```
shared/src/commonMain/kotlin/com/chaekchaek/app/
├── domain
│   ├── book        Book, BookId, PageNumber, PageCount, BookRepository
│   ├── shelf       ShelfBook, Shelf, ReadingStatus, ReadingProgress, ShelfRepository
│   ├── note        Note, VisibleNote, Quote, Reply, NoteAuthor, NoteRepository
│   ├── rating      Rating, RatingSummary, RatedBook, RatingRepository
│   ├── reader      ReaderProfile, Nickname, Viewer, GuestQuota, ReaderRepository
│   ├── feed        HomeFeed, FeedSection, FeedRepository
│   └── auth        AuthRepository
├── data
│   ├── remote
│   │   ├── api     Ktor 클라이언트 래퍼
│   │   └── dto     @Serializable DTO + toDomain()
│   ├── datasource  XxxDataSource(interface) + XxxRemoteDataSource
│   ├── fake        XxxFakeDataSource
│   ├── local       DataStore (게스트 쿼터)
│   └── repository  XxxRepositoryImpl
├── presentation
│   ├── home        HomeViewModel, HomeUiState, FeedSectionUiModel
│   ├── search      SearchViewModel, SearchUiState
│   ├── bookdetail  BookDetailViewModel, NoteUiModel, 다이얼로그 상태
│   ├── shelf       ShelfViewModel, ShelfUiState
│   └── common      AppError, UiEvent, 라벨 포맷터
└── di              kotlin-inject Component, 모듈

androidApp/src/main/kotlin/com/chaekchaek/app/
├── ui              화면별 Composable
├── navigation      NavKey, NavDisplay
└── theme           Color, Type, Theme

iosApp/
└── (SwiftUI 뷰)
```

`presentation`이 화면 단위로 나뉘고, 각 플랫폼의 UI가 같은 이름을 따라간다.

**패키지명은 `com.chaekchaek.app`을 유지한다.** Play Console에 등록된 `applicationId`는
`com.chamsae.chaekchaek`으로 별개이며 변경하지 않는다(`android/CLAUDE.md`의 고정 사항).
`shared`와 `androidApp`이 같은 패키지 루트를 쓰되 하위 경로로 구분한다.

## 5. 데이터 레이어

### 5.1 계층

```
domain/note/NoteRepository.kt          interface (도메인 언어)
        ↑
data/repository/NoteRepositoryImpl.kt  DTO → 도메인 매핑
        ↓
data/datasource/NoteDataSource.kt      interface (DTO 반환)
        ├── NoteRemoteDataSource.kt    Ktor 호출
        └── NoteFakeDataSource.kt      더미 DTO 반환
```

**DataSource는 DTO를 반환하고 Repository가 도메인으로 바꾼다.** 참고 프로젝트
(`wtc/android-shopping-order`)는 DataSource가 도메인을 반환해서 Repository가 한 줄 위임만 하는
빈 계층이 되었다. 같은 실수를 하지 않는다.

인터페이스는 도메인 언어로 쓴다.

```kotlin
// commonMain/domain/note/NoteRepository.kt
interface NoteRepository {
    suspend fun notes(bookId: BookId, sort: NoteSortOrder, scope: NoteScope): List<Note>

    suspend fun write(bookId: BookId, draft: NoteDraft): Note

    suspend fun like(noteId: NoteId): Note

    suspend fun reply(noteId: NoteId, content: String): Reply
}

enum class NoteSortOrder { LATEST, POPULAR }   // Figma: 최신순 / 인기순
enum class NoteScope { ALL, MINE }             // Figma: 전체 피드 / 내 피드
```

### 5.2 Fake

Fake는 **DataSource 레벨**에 둔다. Repository 레벨에 두면 매핑 코드가 서버 붙는 날까지 한 번도
실행되지 않는다. DataSource에 두면 개발 내내 `dto.toDomain()`이 돌아 매핑 버그를 미리 잡는다.

`commonMain`에 두므로 **양 플랫폼이 같은 더미 데이터로 개발한다.** Fake가 반환하는 DTO는
[API 계약](../../docs/api-contract.md)의 예시 응답과 같은 모양이어야 한다.

```kotlin
// commonMain/data/fake/NoteFakeDataSource.kt
@Inject
class NoteFakeDataSource : NoteDataSource {
    override suspend fun notes(bookId: String, sort: String, scope: String): List<NoteDto> {
        delay(FAKE_DELAY_MILLIS)
        return FakeNotes.of(bookId)
    }
}
```

더미 표지 이미지는 저장소의 `images/`를 쓴다(이슈 #7의 요구). Android는 리소스로, iOS는 에셋으로
각각 넣어야 하므로 **경로가 아니라 식별자를 DTO에 담고 각 플랫폼이 해석**한다.

**검증되지 않는 것**: JSON 파싱, HTTP 오류 코드, 페이징, 인증 헤더.

### 5.3 서버 전환

DI 모듈에서 바인딩 한 줄을 바꾼다.

```kotlin
@Provides
fun noteDataSource(impl: NoteFakeDataSource): NoteDataSource = impl
//                       ^^^^^^^^^^^^^^^^^^ 서버 준비 시 NoteRemoteDataSource
```

## 6. 의존성 주입 (kotlin-inject)

Hilt는 Dagger 기반이라 Kotlin/Native를 지원하지 않는다. KMP 선택지는 Koin(런타임),
kotlin-inject(컴파일타임), Metro 등이며, **의존성 누락을 컴파일 시점에 잡기 위해
kotlin-inject**를 택했다.

### 6.1 Component

```kotlin
// commonMain/di/AppComponent.kt
@Component
abstract class AppComponent {
    abstract val homeViewModel: HomeViewModel
    abstract val searchViewModel: SearchViewModel
    abstract val shelfViewModel: ShelfViewModel

    @Provides
    fun noteDataSource(impl: NoteFakeDataSource): NoteDataSource = impl

    @Provides
    fun noteRepository(impl: NoteRepositoryImpl): NoteRepository = impl
}
```

인자를 받는 ViewModel(책 상세의 `bookId`)은 팩토리를 노출한다.

```kotlin
abstract val bookDetailViewModelFactory: (BookId) -> BookDetailViewModel
```

### 6.2 Android에서 꺼내기

kotlin-inject에는 `hiltViewModel()` 같은 것이 없다. Component를 `CompositionLocal`로 내려보내고
화면이 거기서 꺼낸다.

```kotlin
// androidApp
val LocalComponent = staticCompositionLocalOf<AppComponent> {
    error("AppComponent가 제공되지 않았습니다.")
}

@Composable
fun ShelfScreen(
    onBookClick: (BookId) -> Unit,
    viewModel: ShelfViewModel = viewModel { LocalComponent.current.shelfViewModel },
) { ... }
```

`viewModel { }`로 감싸는 이유는 구성 변경(화면 회전) 시 인스턴스를 유지하기 위해서다.

### 6.3 iOS에서 꺼내기

Swift가 같은 Component에서 꺼낸다.

```swift
let viewModel = component.shelfViewModel
```

구조가 대칭이라 양쪽이 같은 그래프를 본다.

## 7. presentation 레이어

### 7.1 UiState

기본은 **sealed 3상태**다.

```kotlin
sealed interface ShelfUiState {
    data object Loading : ShelfUiState

    data class Failure(val error: AppError) : ShelfUiState

    data class Content(
        val books: List<ShelfBookUiModel>,
        val filter: ReadingStatus?,
        val countLabel: String,
        val editing: ShelfEditUiModel?,
    ) : ShelfUiState
}
```

**부분 갱신이 실제로 있는 화면만** `Content` 안에 하위 상태를 중첩한다. 책 상세가 그렇다. 감상
정렬이나 범위를 바꾸면 감상 목록만 다시 불러오고 표지·별점·내 독서 기록은 그대로 있어야 한다.

```kotlin
sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Failure(val error: AppError) : BookDetailUiState
    data class Content(
        val book: BookUiModel,
        val myRecord: ReadingRecordUiModel,
        val notes: NoteListUiState,      // 중첩
        val dialog: BookDetailDialog?,
    ) : BookDetailUiState
}
```

`sealed`를 쓰는 이유가 KMP에서 하나 더 생겼다. SKIE 같은 브리지를 붙이면 **Swift에서 exhaustive
enum이 되어 분기 누락이 컴파일 오류가 된다.** 브리지를 붙이지 않으면 iOS에서는 런타임 확인이
되므로, 이 점이 브리지 선택의 판단 근거가 된다.

### 7.2 UiModel

**ViewModel이 도메인을 UiModel로 바꾼다.** UI는 도메인 타입을 모른다.

가려진 감상은 **본문 문자열을 담지 않는다.** 화면에 도달하지 않은 문자열은 실수로도 그릴 수 없다.

```kotlin
sealed interface NoteUiModel {
    val id: NoteId
    val authorLabel: String       // "참새 1204 (익명)" 또는 "골똘한 참새"
    val dateLabel: String         // "2026.08.05"

    data class Visible(
        override val id: NoteId,
        override val authorLabel: String,
        override val dateLabel: String,
        val readingPointLabel: String,  // "p.80까지"
        val completedBadge: Boolean,
        val impression: String,
        val quote: QuoteUiModel?,
        val likeLabel: String,          // "좋아요 12"
        val likedByMe: Boolean,
        val replyLabel: String,         // "답글 2"
        val replies: List<ReplyUiModel>,
    ) : NoteUiModel

    data class Hidden(
        override val id: NoteId,
        override val authorLabel: String,
        override val dateLabel: String,
        val requiredPageLabel: String,  // "160쪽"
    ) : NoteUiModel                     // 본문·인용문 없음
}
```

`id`에 `NoteId`(value class)가 남아 있는데, 이건 **UI가 값을 읽지 않고 콜백으로 돌려주기만
하는 불투명한 토큰**이라 Swift에서 `Any`로 보여도 문제가 없다. 표시에 쓰이는 값은 전부 `String`
이다.

### 7.3 표시 문자열

UiModel의 라벨은 `commonMain`에서 만든다. Android의 `strings.xml`도 iOS의 `Localizable.strings`도
쓰지 않는다. **한국어 전용 앱**을 전제로 한 결정이다.

```kotlin
// commonMain/presentation/common/Labels.kt
object ShelfLabels {
    fun progress(progress: ReadingProgress): String =
        "${progress.currentPage.value}쪽 / ${progress.totalPages.value}쪽"

    fun count(count: Int): String = "전체 ${count}권"
}

object NoteLabels {
    fun readingPoint(page: PageNumber): String = "p.${page.value}까지"

    fun author(author: NoteAuthor): String = when (author) {
        is NoteAuthor.Named -> author.nickname.value
        is NoteAuthor.Anonymous -> "${author.handle} (익명)"
    }
}
```

포맷터를 `object`로 모아두면 나중에 다국어가 필요해질 때 여기만 바꾸면 된다. 문구가 코드에
흩어지지 않게 **UiModel 매핑에서 직접 문자열을 조립하지 않고 반드시 이 포맷터를 거친다.**

### 7.4 ViewModel

`androidx.lifecycle.ViewModel`이 2.8.0-alpha03부터 KMP 아티팩트로 배포되어 `commonMain`에서 쓸 수
있다. 현재 프로젝트가 이미 2.10.0을 쓰고 있다.

```kotlin
@Inject
class ShelfViewModel(
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
- 화면 전환은 ViewModel이 하지 않는다. UI가 콜백으로 처리한다
- `viewModelScope`를 쓰되, iOS에서 생명주기를 직접 끊어줘야 하므로 `clear()` 호출 시점을 SwiftUI
  쪽에서 관리한다 (구체적인 방법은 iOS 착수 시 확정)

## 8. UI 레이어

### 8.1 Android (Compose)

- `NavController`/`NavBackStack`을 받지 않는다. `onBookClick: (BookId) -> Unit` 같은 콜백을 받는다
- 상태는 `collectAsStateWithLifecycle()`로 받는다
- 화면 Composable(상태 연결)과 내용 Composable(상태 없음)을 나눈다. 내용 쪽에 `@Preview`를 붙인다

```kotlin
@Composable
fun ShelfScreen(onBookClick: (BookId) -> Unit, viewModel: ShelfViewModel = ...) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShelfContent(uiState = uiState, onBookClick = onBookClick, ...)
}

@Composable
private fun ShelfContent(uiState: ShelfUiState, ...) { ... }
```

### 8.2 iOS (SwiftUI)

`StateFlow`를 SwiftUI가 직접 관찰할 수 없어 브리지가 필요하다. 후보는 SKIE(sealed → Swift enum
변환 포함)와 KMP-NativeCoroutines(Flow → async/await·Combine)이며, **iOS 착수 시점에 정한다.**

브리지를 SKIE로 정하면 UI 코드가 이렇게 된다.

```swift
switch onEnum(of: model) {
case .visible(let m): NoteCard(model: m)
case .hidden(let m):  LockedNoteCard(model: m)
}
```

### 8.3 내비게이션

내비게이션은 **공유하지 않는다.** Android는 Navigation3, iOS는 `NavigationStack`을 각자 쓴다.
공유하는 것은 「어느 화면으로 가야 하는가」가 아니라 「무슨 일이 일어났는가」다.

Figma에서 책 상세·서재 편집에는 하단 탭바가 없고 홈과 내 서재에만 있다. 상세가 탭 위로 올라오는
전체 화면이라는 뜻이므로, 탭은 백스택에 넣지 않는다.

```
Android backStack
  [RootKey]                                  탭 컨테이너 (탭바 보임)
  [RootKey, BookDetailKey(bookId)]           책 상세 (탭바 없음)
  [RootKey, BookDetailKey, NoteComposeKey]   감상 작성
```

```kotlin
enum class RootTab { HOME, DISCOVER, SHELF }   // Figma: 홈 / 발견 / 내 서재
```

「발견」 탭은 검색 화면(36:427)을 쓴다. 시안에 탭바가 없으므로 **하단에 탭바를 추가**해야 한다.

**감수한 한계**: 탭별 독립 백스택이 없다. 홈에서 상세를 열고 다른 탭에 갔다 돌아오면 상세가
아니라 탭 목록이 보인다.

## 9. 오류 처리

```kotlin
// commonMain/presentation/common/AppError.kt
sealed interface AppError {
    data object Network : AppError
    data object NotFound : AppError
    data object Unauthorized : AppError
    data object Unknown : AppError
}
```

예외를 `AppError`로 바꾸는 함수는 Ktor 예외 타입에 의존하므로 `data` 레이어에 둔다. presentation은
`AppError`만 안다.

**표시 방식은 다이얼로그다.** Figma에 오류 시안이 없어 새로 만든다. 확인·재시도 버튼을 두고,
재시도가 의미 없는 오류(`NotFound`)는 확인만 둔다.

부분 실패(감상 목록만 실패)는 화면 전체가 아니라 해당 영역의 상태를 `Failure`로 만든다.

## 10. 테스트

### 10.1 도구

```kotlin
// shared/build.gradle.kts
commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.kotest.assertions.core)
    implementation(libs.kotlinx.coroutines.test)
}
```

JUnit5는 JVM 전용이라 `commonTest`에서 쓸 수 없다. `kotlin.test`를 쓰면 같은 테스트가 JVM과
iOS 시뮬레이터 양쪽에서 돈다.

### 10.2 무엇을 테스트하나

| 대상 | 위치 | 우선순위 |
| --- | --- | --- |
| 도메인 불변식·상태 전이 | `commonTest`. [규칙 목록 30개](domain-model.md#10-규칙-목록-테스트-대상) | 높음 |
| 스포일러 판정 | `commonTest`. 체크 여부와 감상별 공개 범위 | 높음 |
| ViewModel | `commonTest`. Fake Repository + `runTest` | 중간 |
| DTO 매핑 | `commonTest` | 중간 |
| 가림 문자열 변환 | `commonTest`. 원문 길이와 공백, 문장부호 유지 | 높음 |
| Compose UI | `androidApp`. 핵심 흐름만 | 낮음 |
| SwiftUI | `iosApp`. iOS 착수 후 | 낮음 |

ViewModel 테스트용 Fake는 `commonTest`에 둔다. `commonMain`의 `data/fake`(개발용 더미)와 목적이
다르므로 공유하지 않는다. 개발용 Fake는 "그럴듯한 화면"을 만들고, 테스트용 Fake는 "특정 조건"을
만든다.

### 10.3 스타일

한글 백틱 함수명과 given/when/then 주석을 쓴다.

```kotlin
@Test
fun `다 읽음으로 바꾸면 진행 쪽수가 총 쪽수가 된다`() {
    // given : 308쪽 중 80쪽을 읽은 책이 서재에 있다
    val shelfBook = shelfBook(current = 80, total = 308)

    // when : 상태를 다 읽음으로 바꾸면
    val finished = shelfBook.changeStatus(ReadingStatus.FINISHED, at = FIXED_INSTANT)

    // then : 진행 쪽수가 308쪽이 된다
    finished.progress.currentPage shouldBe PageNumber(308)
}
```

시각이 들어가는 객체는 `Instant`를 인자로 받게 만들어 테스트에서 고정값을 넣는다.
`Clock.System.now()`를 도메인 안에서 부르지 않는다.

## 11. 기존 코드 마이그레이션

현재 `app/src/main`은 811줄이고 레이어가 없다. KMP 전환과 함께 옮긴다.

| 현재 | 이동 후 | 비고 |
| --- | --- | --- |
| `data/Book.kt` | `shared/domain/book/Book.kt` + `shared/data/remote/dto/BookDto.kt` | `org.json` → kotlinx.serialization |
| `data/BookSearchApi.kt` | `shared/data/remote/api/BookApi.kt` | `HttpURLConnection` → Ktor. `object` → 주입 가능한 클래스 |
| `data/ArchiveRepository.kt` | `shared/data/repository/ShelfRepositoryImpl.kt` | SharedPreferences → multiplatform-settings 또는 DataStore |
| `ui/search/*` | `shared/presentation/search/*` + `androidApp/ui/search/*` | 상태와 UI 분리 |
| `ui/archive/*` | `shared/presentation/shelf/*` + `androidApp/ui/shelf/*` | |
| `RootScreen.kt` | `androidApp/ui/root/RootScreen.kt` | 탭 2개 → 3개 |
| `Navigation.kt`, `NavigationKeys.kt` | `androidApp/navigation/*` | |
| `theme/*` | `androidApp/theme/*` | iOS는 별도 |
| `MainActivity.kt` | `androidApp/MainActivity.kt` | |

현재 코드에서 함께 고칠 문제들이다.

1. `BookSearchApi`가 `object` 싱글턴이라 테스트에서 바꿔 끼울 수 없고 `BuildConfig`를 직접
   참조한다. `BuildConfig`는 Android 전용이므로 KMP에서는 `expect/actual`이나 생성자 주입으로
   바꿔야 한다
2. `ArchiveRepository(context)`가 `RootScreen`의 `remember { }`에서 생성된다
   (`RootScreen.kt:32`). 화면이 의존성을 만들면 생명주기와 테스트가 꼬인다. DI로 옮긴다
3. `BookSearchApi.search`의 `catch (e: IOException) { fetch(url) }`은 재시도 1회를 예외 처리로
   표현해 의도가 드러나지 않는다. Ktor의 `HttpRequestRetry`로 대체한다
4. `data/Book.kt`에 모델·JSON 파싱·직렬화가 한 파일에 섞여 있다
5. `org.json`은 JVM 전용이라 KMP에서 쓸 수 없다

## 12. 구현 순서 (이슈 분할안)

프로젝트 규칙상 이슈는 "사용자가 완료된 동작 하나를 확인할 수 있는 단위"다. 아래는 제안이며
확정 전에 함께 조정한다.

| 순서 | 이슈 | 확인 가능한 동작 |
| --- | --- | --- |
| 0 | KMP 모듈 전환 | `shared`·`androidApp` 구조에서 기존 앱이 그대로 동작한다 |
| 1 | 도메인 모델 + 테스트 | 규칙 30개가 `commonTest`에서 통과한다 |
| 2 | 탭 3개 + 홈 피드 (이슈 #7) | 앱을 켜면 홈 피드가 보이고 탭 3개로 이동한다 |
| 3 | 검색(발견 탭) | 책을 검색하고 「읽는 중 시작」으로 서재에 담는다 |
| 4 | 내 서재 목록 | 상태 필터와 정렬로 서재를 본다 |
| 5 | 책 상세 + 내 독서 기록 | 상태를 바꾸고 쪽수를 기록한다 |
| 6 | 감상 목록 + 스포일러 가드 | 체크된 감상을 가리고 선택한 감상만 공개한다 |
| 7 | 감상 작성 | 감상을 남기면 목록에 나타난다 |
| 8 | 답글 + 좋아요 | 감상에 답글을 달고 좋아요를 누른다 |
| 9 | 별점 | 별점을 매기고 최근 기록을 본다 |
| 10 | 서재 편집 모드 | 여러 권을 골라 상태를 바꾸거나 지운다 |
| 11 | 닉네임·익명 설정 | 익명 공개를 끄고 닉네임을 정한다 |
| 12 | 게스트 열람 제한 | 감상 3개를 보면 배너가 소진 상태가 된다 |
| 13+ | iOS 앱 착수 | 브리지 결정 후 SwiftUI로 같은 화면을 그린다 |

0과 1은 화면 변화가 없어 이슈 단위 규칙에서 벗어난다. 다른 작업의 선행 조건이라 예외로 둔다.

**iOS 착수 시점**: Android로 화면 몇 개를 완성해 UiModel 형태가 안정된 뒤가 좋다. 너무 이르면
UiModel이 계속 바뀌어 양쪽을 동시에 고치게 된다.

## 부록 A. 참고 프로젝트에서 바꾼 것

`wtc/android-shopping-order`를 참고하되 아래는 의도적으로 다르게 간다. 자세한 근거는
[도메인 모델 부록 A](domain-model.md#부록-a-참고-프로젝트에서-바꾼-것)에 있다.

1. Repository 인터페이스를 `domain`에 둔다 (참고 프로젝트는 `data`에 두어 `feature`가 `data`를
   import한다)
2. 감싼 값을 도로 꺼내지 않는다 (`Money` → `priceAmount(): Int` 같은 유출을 막는다)
3. 컬렉션을 감춘다 (`Cart.cartContents`가 `public`이라 ViewModel이 내부를 순회한다)
4. 도메인에 Android를 들이지 않는다 (`domain/Product.kt`에 `import android.R.attr.name`이 있다)
5. Repository가 실제로 매핑 일을 한다 (참고 프로젝트는 한 줄 위임뿐이다)

## 부록 B. KMP 때문에 바뀐 결정

Android 단독 설계에서 정했다가 KMP 전환으로 뒤집힌 것들이다.

| 항목 | 이전 | 이후 | 이유 |
| --- | --- | --- | --- |
| DI | Hilt | kotlin-inject | Hilt는 Dagger 기반이라 KMP 미지원 |
| 네트워크 | Retrofit | Ktor Client | Retrofit은 JVM 전용 |
| 시각 | `java.time` | kotlinx-datetime | `java.*`는 Kotlin/Native에 없음 |
| JSON | `org.json` | kotlinx.serialization | `org.json`은 JVM 전용 |
| 로컬 저장 | SharedPreferences | multiplatform-settings 또는 DataStore | Android 전용 API |
| 테스트 | JUnit5 + AssertJ | kotlin.test + Kotest assertions | JUnit5는 JVM 전용 |
| 모듈 | 단일 `:app` | `:shared` + `:androidApp` + `:iosApp` | |
| UiModel 위치 | Android `feature` | 공유 `presentation` | 스포일러 판정 단일화, value class 노출 차단 |

**살아남은 결정**: 도메인 모델 전체, Repository 인터페이스를 domain에 두기, 상태-쪽수 불변식,
감상 쪽수 분리, sealed UiState, UiModel 변환, Fake를 DataSource에 두기, 스포일러 정책.

특히 **Repository 인터페이스를 `domain`에 둔 결정은 KMP에서 오히려 필수**가 되었다. 그 배치가
아니었다면 `:shared` 분리가 순환 참조로 막혔을 것이다.
