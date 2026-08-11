# 구현 인수인계 (2026-08-11)

Claude Code 세션에서 진행하던 KMP 전환과 이슈 #7 구현을 다른 도구(Codex 등)가 이어받기 위한
문서다. **무엇이 끝났고, 어떤 리듬으로 일하고 있었고, 다음에 무엇을 하는지**를 적는다.

설계 자체는 아래 문서에 있다. 이 문서는 진행 상태만 다룬다.

- [도메인 모델](domain-model.md) - 객체와 규칙, 테스트 대상 목록
- [앱 아키텍처](app-architecture.md) - 레이어, DI, 상태 규칙
- [KMP 셋업](kmp-setup.md) - 모듈 구성, **검증된 버전 조합**
- [화면 명세](screen-specs.md) - 화면 12개의 상태·액션
- [API 계약](../../docs/api-contract.md) - 백엔드와 공유

## 1. 현재 위치

| 항목 | 값 |
| --- | --- |
| 브랜치 | `feat/7-home-feed` |
| 대상 이슈 | [#7 홈 피드와 모바일 내비게이션 구현](https://github.com/woowacourse-teams/2026-chaekchaek/issues/7) |
| 이 브랜치의 커밋 | 4개 (아래) |
| 테스트 | **52개 통과** (JVM + iOS 시뮬레이터 양쪽) |

```
44b36ae feat(shared): 서재 컬렉션 Shelf 추가
6850737 feat(shared): KMP 공유 모듈과 도메인 값 객체 추가
830b7fb docs(android): 설계를 KMP 기준으로 전환
14a4da4 docs(android): 앱 아키텍처와 도메인 모델 설계 문서 추가
```

이 브랜치는 아직 원격에 push 되지 않았다.

### 확인 명령

```bash
cd android
./gradlew :shared:allTests        # 도메인 테스트 (JVM + iOS)
./gradlew assembleDebug           # 기존 앱 빌드 (여전히 동작)
```

테스트 결과는 `shared/build/test-results/testAndroidHostTest/` 에 XML 로 쌓인다.

## 2. 끝난 것

### 2.1 `:shared` KMP 모듈

기존 `:app` 을 **건드리지 않고** 옆에 새로 추가했다. 앱은 그대로 빌드되고 실행된다.

```
android/
├── app/            기존 Android 앱 (그대로)
├── shared/         새로 추가한 KMP 모듈
│   └── src/
│       ├── commonMain/kotlin/com/chaekchaek/app/
│       │   ├── di/SharedComponent.kt
│       │   └── domain/
│       │       ├── book/    Book, BookId, Page(PageNumber/PageCount)
│       │       ├── note/    NoteIds(NoteId/ReplyId)
│       │       ├── rating/  Rating, RatingSummary
│       │       ├── reader/  Nickname, ReaderId
│       │       └── shelf/   ReadingProgress, ReadingStatus, ShelfBook, Shelf
│       └── commonTest/kotlin/com/chaekchaek/app/
│           ├── domain/...   테스트
│           └── fixture/ShelfFixture.kt
└── settings.gradle.kts   include(":app"), include(":shared")
```

### 2.2 검증된 버전 조합

설계 문서에 "미확인"으로 남겼던 최대 위험이 해소되었다. **실제로 빌드해서 확인했다.**

| 항목 | 버전 | 확인 |
| --- | --- | --- |
| Kotlin | 2.3.20 | |
| AGP | 9.0.1 | |
| Gradle | 9.1.0 | |
| KSP | 2.3.11 | 4개 타겟 코드 생성 성공 |
| kotlin-inject | 0.9.0 | Kotlin 2.2.20 기준 빌드본이나 2.3.20에서 동작 |
| kotlinx-datetime | 0.8.0 | |
| Kotest assertions | 6.2.3 | commonTest 동작 |

Ktor, 로컬 저장, 이미지 로더, SKIE 는 **아직 미검증**이다.

### 2.3 구현된 도메인

[도메인 모델의 규칙 목록](domain-model.md#10-규칙-목록-테스트-대상) 중 아래가 코드와 테스트로
존재한다.

| 규칙 | 대상 | 상태 |
| --- | --- | --- |
| 1~3 | 쪽수 | 완료 |
| 4~10, 10-1, 10-2 | 독서 상태·불변식 | 완료 |
| 11~13, 13-1, 13-2 | 서재 | 완료 |
| 14~16 | 별점 | 완료 |
| 25, 25-1~3 | 닉네임 | 완료 |
| 17~19 | 감상 | **미구현** |
| 20~24 | 스포일러 | **미구현** |
| 26~27 | 정체성(ReaderProfile) | **미구현** |
| 28~30 | 게스트 쿼터 | **미구현** |

## 3. 작업 리듬 (이어받을 때 지킬 것)

사용자와 합의한 진행 방식이다. **이 리듬을 유지한다.**

### 3.1 단계마다 구분해서 보고

완료한 것과 다음에 할 것을 시각적으로 구분한다. 구분선(`---`)과 제목을 쓴다.

```markdown
---
# ✅ 완료: <단계 이름>

## 만든 파일
| 파일 | 내용 |

## 검증 결과
테스트 N개 통과

## 알아낸 것
(빌드하며 발견한 사실)

---
# ⏭️ 다음: <단계 이름>

## 만들 파일
## 핵심 규칙 (코드 미리보기)
---
```

### 3.2 미결정 사항은 다음 단계로 넘기지 않는다

한 단계에서 "확인이 필요한 결정"이 나오면 **그 단계를 끝내기 전에 해소한다.** 쌓아두고 넘어가지
않는다.

**결정은 반드시 사용자에게 질문해 명시적으로 승인받는다.** 사용 중인 도구에 질문 UI가 있으면
이를 사용한다. 선택지마다 코드 미리보기(`preview`)를 붙여 무엇이 달라지는지 보이게 한다.

결정이 나면 **코드와 문서를 함께 고친다.** 문서의 「미결정 사항」 표에서 그 줄을 지운다.

### 3.3 파일 하나하나 확인

코드를 쓰기 전에 무엇을 만들지 보여주고 진행한다. 여러 파일을 한꺼번에 만들고 나서 통보하지
않는다.

### 3.4 단계마다 커밋

한 단계(파일 몇 개 ~ 한 계층)가 끝나고 테스트가 통과하면 그 시점에 커밋한다. Conventional
Commits 형식, 한글 본문, 서명 트레일러 없음.

### 3.5 사실은 근거와 함께

라이브러리 동작이나 사양을 설명할 때 추측으로 단정하지 않는다. 실제로 빌드해보거나 문서를
확인하고, 확인 못 한 것은 "미확인"이라고 밝힌다.

## 4. 빌드하며 알아낸 함정

문서에 반영해두었지만 다시 강조한다.

1. **AGP 9.0부터 `com.android.library` 와 KMP 플러그인을 함께 쓸 수 없다.**
   `com.android.kotlin.multiplatform.library` 와 `androidLibrary { }` DSL 을 쓴다.
2. **KMP·KSP 플러그인은 루트 `build.gradle.kts` 에 `apply false` 로 선언해야 한다.**
   Kotlin 플러그인이 이미 classpath 에 있어 버전 충돌이 난다.
3. **`androidLibrary { withHostTestBuilder {} }` 를 켜야 JVM 테스트 태스크가 생긴다.**
   켜지 않으면 `allTests` 가 iOS 시뮬레이터에서만 돌아 Android 문제를 놓친다.
4. **`Instant` 는 `kotlinx.datetime` 이 아니라 `kotlin.time` 에 있다.**
5. **백틱 테스트 함수명에 마침표를 쓸 수 없다.** `0.5 단위가...` → 컴파일 오류.
   「반개 단위」처럼 우회한다.
6. **`.gitignore` 의 `/build` 는 최상위만 무시한다.** 하위 모듈용으로 `build/` 를 추가했다.

## 5. 주의: 다른 도구의 미커밋 변경

작업 트리에 **다른 도구/세션이 남긴 미커밋 변경**이 있다. 건드리지 않았다.

| 파일 | 변경 |
| --- | --- |
| `android/app/build.gradle.kts` | `applicationId` → `com.chamsae.chaekchaek`, versionCode 2 |
| `android/app/src/.../ui/search/SearchScreen.kt` | 135줄 수정 |
| `android/app/src/main/res/**` | 런처 아이콘 |
| `android/app-logo.png`, `app-logo-square.png` | 신규 |
| `AGENTS.md`, `android/AGENTS.md`, `android/README.md` | 문서 수정 |
| `designs.pen`, `wireframes-app` 등 | 디자인 파일 |

**이것 때문에 `app/` → `androidApp/` 이름 변경을 미뤘다.** 지금 디렉터리를 옮기면 저 작업물이
꼬인다. 작업 트리가 정리된 뒤에 [KMP 셋업 5절](kmp-setup.md#5-전환-절차)의 이름 변경을 진행한다.

단일 모듈 + 수동 DI 전제라 KMP 설계와 충돌하던 `docs/home-feed-architecture.md` 는 삭제했고,
쓸 만한 내용은 `screen-specs.md` 와 `kmp-setup.md` 로 옮겼다. `android/README.md` 의 예전 링크는
이 인수인계 문서 링크로 교체했다.

## 6. 다음에 할 일

이슈 #7 완료 조건은 **홈 피드 표시 / 탭 3개 이동 / 더미 데이터로 도서 목록 표시**다.
서재·감상·스포일러는 이 이슈 범위 밖이다.

재개할 때는 6.1의 두 파일에 대한 API·테스트 미리보기를 먼저 보여주고, 사용자 승인 뒤 구현한다.

### 6.1 홈 피드 도메인 + Repository 계약

```
domain/feed/HomeFeed.kt        FeedSection(sealed): Trending, RecentQuotes, Overlapped
domain/feed/FeedRepository.kt  suspend fun homeFeed(): HomeFeed
```

[화면 명세 2절](screen-specs.md)의 상태 정의와 [API 계약 4절](../../docs/api-contract.md#4-홈-피드)의
응답 모양을 따른다. 서버가 섹션 배열을 내려주고 **앱은 모르는 타입을 무시**한다.

### 6.2 Fake DataSource

```
data/remote/dto/FeedDto.kt         @Serializable, API 계약과 같은 모양
data/datasource/FeedDataSource.kt  interface, DTO 반환
data/fake/FeedFakeDataSource.kt    더미 DTO + delay
data/repository/FeedRepositoryImpl.kt  DTO → 도메인 매핑
```

**Fake 는 DataSource 레벨에 둔다.** Repository 레벨에 두면 매핑 코드가 서버 붙는 날까지 한 번도
실행되지 않는다.

더미 데이터는 [화면 명세의 더미 데이터 절](screen-specs.md)을 따른다. 표지는
`images/cover-01.png` ~ `cover-12.png`. **DTO 에는 경로가 아니라 식별자(`cover-01`)를 담고 각
플랫폼이 해석한다.** 상대 시각("4분 전")은 저장하지 않고 `Instant` 를 담아 표시 시점에 계산한다.

### 6.3 HomeViewModel + UiState + UiModel

```
presentation/home/HomeViewModel.kt
presentation/home/HomeUiState.kt
presentation/home/FeedSectionUiModel.kt
presentation/common/Labels.kt     표시 문자열 포맷터
```

`androidx.lifecycle.ViewModel` 을 `commonMain` 에서 쓴다(2.10.0, KMP 지원 확인됨).
라벨 문자열은 매핑에서 직접 조립하지 말고 포맷터를 거친다.

### 6.4 kotlin-inject 그래프 연결

`SharedComponent` 에 Repository 와 ViewModel 을 등록한다. Android 에서는 Component 를
`CompositionLocal` 로 내려보내 화면이 꺼낸다([아키텍처 6.2](app-architecture.md#62-android에서-꺼내기)).

### 6.5 홈 화면 Compose + 탭 3개

```
app/src/main/java/com/chaekchaek/app/ui/home/HomeScreen.kt
app/src/main/java/com/chaekchaek/app/RootScreen.kt   탭 2개 → 3개로 확장
```

현재 `RootScreen` 은 탭이 검색·내 서재 2개이고 `remember { ArchiveRepository(context) }` 로
의존성을 직접 만든다. 탭을 홈·발견·내 서재 3개로 바꾸고 DI 로 옮긴다.

Figma 홈은 [node 36:1206](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr/%EC%B1%85%EC%B7%8D?node-id=36-1206)
이다.

### 6.6 남은 미결정 사항 (이 단계에서 부딪힐 것)

| 항목 | 내용 |
| --- | --- |
| 홈 로딩·오류 UI | Figma 에 시안이 없다. 다이얼로그로 하기로 정했으나 구체안 미정 |
| 게스트 쿼터 소진 표시 | Figma 는 `2 / 3` 상태만 있다 |
| 「모두 보기」·「목록」 목적지 | 시안 없음. 목적지 없는 버튼은 만들지 않기로 함 |
| 알림 아이콘 동작 | 미정 |
| 탭 아이콘 | 현재 텍스트 기호(`⌕`, `▤`). Figma 아이콘으로 교체 필요 |

**부딪히면 그 단계를 끝내기 전에 질문 도구로 결정을 받는다.**

## 7. 전체 로드맵에서의 위치

[아키텍처 12절](app-architecture.md#12-구현-순서-이슈-분할안)의 순서 기준이다.

| 순서 | 이슈 | 상태 |
| --- | --- | --- |
| 0 | KMP 모듈 전환 | **부분 완료** (`:shared` 추가됨, `app/`→`androidApp/` 이름 변경은 보류) |
| 1 | 도메인 모델 + 테스트 | **부분 완료** (책·서재·별점·닉네임 완료, 감상·스포일러·정체성 남음) |
| 2 | 탭 3개 + 홈 피드 (#7) | **다음 차례** |
| 3~12 | 검색·서재·상세·감상·스포일러·별점·편집·닉네임·게스트 | 대기 |
| 13+ | iOS 앱 착수 | 대기 (UiModel 안정된 뒤) |
