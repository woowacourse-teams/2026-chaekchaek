# 첵췍 API 계약 (초안)

Android 앱과 백엔드가 공유하는 약속이다. 서버가 아직 없어서 **앱이 Figma 시안에서 역산한
초안**이며, 백엔드 설계가 시작되면 함께 조정한다. 앱은 이 계약과 같은 모양의 더미 DTO를
`data/fake`에서 반환하며 개발한다.

도메인 개념의 의미는 [Android 도메인 모델](../android/docs/domain-model.md)에 있다.

## 1. 공통 규약

| 항목 | 값 |
| --- | --- |
| 형식 | JSON, UTF-8 |
| 시각 | ISO-8601 UTC (`2026-08-05T12:34:56Z`) |
| 날짜 | ISO-8601 (`2026-08-05`) |
| 식별자 | 문자열 |
| 인증 | `Authorization` 헤더. 방식 미정 (아래 3절) |

### 1.1 오류 응답

모든 4xx·5xx는 같은 모양으로 내려준다.

```json
{
  "code": "BOOK_NOT_FOUND",
  "message": "요청한 책을 찾을 수 없습니다."
}
```

앱은 HTTP 상태 코드로 `AppError`를 정하고, `message`는 로그에만 쓴다. 사용자에게 보여줄 문구는
앱이 가진다.

| 상태 | 앱의 처리 |
| --- | --- |
| 401, 403 | `AppError.Unauthorized` |
| 404 | `AppError.NotFound` |
| 그 외 4xx, 5xx | `AppError.Unknown` |
| 네트워크 실패 | `AppError.Network` |

### 1.2 열거값

문자열 상수로 주고받는다.

| 개념 | 값 |
| --- | --- |
| 독서 상태 | `WANT_TO_READ`, `READING`, `FINISHED` |
| 감상 정렬 | `LATEST`, `POPULAR` |
| 감상 범위 | `ALL`, `MINE` |
| 피드 섹션 | `TRENDING_BOOKS`, `RECENT_QUOTES`, `OVERLAPPED_BOOKS` |

## 2. 미결정 항목

계약을 확정하려면 아래가 먼저 정해져야 한다.

| 항목 | 설명 |
| --- | --- |
| 인증 방식 | Figma에 로그인 화면이 없다. 수단·토큰 형식·갱신 전략 미정 |
| 익명 핸들의 수명 | `참새 1204`가 사용자마다 고정인지 감상마다 새로 생기는지 |
| 「인기순」의 정의 | 좋아요 수인지, 답글 포함인지, 시간 가중이 있는지 |
| 「지금 인기 책들」 산출 | 기간과 기준 |
| 「밑줄이 겹친 책」 정의 | 같은 구간 인용이 몇 건 이상일 때 겹친 것으로 보는지 |
| 페이징 | 감상 30개·검색 결과의 페이지 크기와 방식(offset/cursor) |
| 책 등록 경로 | 검색 결과의 책이 우리 DB에 없을 때 누가 만드는지 |

## 3. 인증

앱은 경계만 정의하고 방식은 서버 결정을 따른다. 지금은 모든 요청에 `Authorization` 헤더 자리를
비워두고, 비로그인 상태에서도 아래 GET 요청이 동작해야 한다.

**비로그인으로 접근 가능해야 하는 것**: 홈 피드, 책 검색, 책 상세, 감상 목록.
**로그인이 필요한 것**: 감상 작성, 답글, 좋아요, 서재 조작, 별점, 프로필.

게스트 열람 제한(3개)은 **앱이 기기에 저장해서 센다.** 서버는 게스트 카운트를 모른다.

## 4. 홈 피드

### GET /feed/home

섹션 배열을 순서대로 내려준다. 앱은 배열 순서대로 그린다.

```json
{
  "sections": [
    {
      "type": "TRENDING_BOOKS",
      "totalCount": 12,
      "books": [
        {
          "id": "bk_001",
          "title": "보이지 않는 도시",
          "coverUrl": "https://cdn.chaekchaek.app/covers/bk_001.png",
          "noteCount": 128,
          "replyCount": 46
        }
      ]
    },
    {
      "type": "RECENT_QUOTES",
      "quotes": [
        {
          "noteId": "nt_1001",
          "bookId": "bk_001",
          "bookTitle": "보이지 않는 도시",
          "coverUrl": "https://cdn.chaekchaek.app/covers/bk_001.png",
          "authorLabel": "김여름의 서재",
          "createdAt": "2026-08-07T04:26:00Z",
          "quoteText": "도시는 기억으로 만들어진다는 문장에서 오래 멈췄다. 떠난 장소도 읽는 동안은 다시 현재가 된다.",
          "replyCount": 12
        }
      ]
    },
    {
      "type": "OVERLAPPED_BOOKS",
      "books": [
        {
          "bookId": "bk_002",
          "title": "역병",
          "coverUrl": "https://cdn.chaekchaek.app/covers/bk_002.png",
          "noteCount": 96,
          "authorLabel": "윤서의 서재",
          "createdAt": "2026-08-07T01:00:00Z",
          "excerpt": "무너지는 세계에서 서로를 돌보는 일은 거창한 구원이 아니라 매일의 선택이었다.",
          "replyCount": 28
        }
      ]
    }
  ]
}
```

**앱의 처리 규칙**

- 모르는 `type`은 **무시하고 건너뛴다.** 서버가 새 섹션을 추가해도 구버전 앱이 죽지 않아야 한다.
- 빈 섹션(`books`/`quotes`가 빈 배열)은 그리지 않는다.
- `authorLabel`은 서버가 완성해서 내려준다(`김여름의 서재`). 앱이 조립하지 않는다.

`kotlinx.serialization`의 다형 역직렬화를 쓰되, 모르는 타입에서 예외가 나지 않도록 커스텀
처리가 필요하다. 구현 시 확인한다.

## 5. 책

### GET /books/search

```
GET /books/search?query=마션&page=0&size=20
```

```json
{
  "totalCount": 10,
  "page": 0,
  "last": true,
  "books": [
    {
      "id": "bk_003",
      "title": "마션",
      "authors": ["앤디 위어"],
      "translators": ["박아람"],
      "publisher": "알에이치코리아",
      "category": "SF",
      "publishedYear": 2026,
      "totalPages": 308,
      "coverUrl": "https://cdn.chaekchaek.app/covers/bk_003.png",
      "noteCount": 46,
      "shelfStatus": null
    }
  ]
}
```

`shelfStatus`는 로그인 상태에서 내 서재에 있으면 상태값, 없으면 `null`이다. 검색 결과의
「읽는 중 시작」 버튼 표시에 쓴다.

**검토 필요**: 현재 앱은 알라딘 Open API를 직접 호출한다
(`android/app/src/main/java/com/chaekchaek/app/data/BookSearchApi.kt`). 검색을 서버로 옮길지
앱이 계속 직접 호출할지 정해야 한다.

서버로 옮기면 이점이 둘이다. 첫째, TTBKey가 앱 바이너리에서 사라진다
(`android/AGENTS.md`에 기록된 한계 해소). 둘째, **iOS 앱에서 키를 다시 심을 필요가 없다.**
앱이 직접 호출하면 Android는 `BuildConfig`, iOS는 `Info.plist`로 키를 각각 주입해야 하고,
두 곳에서 키가 유출될 수 있다.

### GET /books/{bookId}

```json
{
  "id": "bk_003",
  "title": "마션",
  "authors": ["앤디 위어"],
  "translators": ["박아람"],
  "publisher": "알에이치코리아",
  "category": "SF",
  "publishedYear": 2026,
  "edition": "2026 초판",
  "totalPages": 308,
  "coverUrl": "https://cdn.chaekchaek.app/covers/bk_003.png",
  "rating": {
    "average": 4.2,
    "raterCount": 100
  },
  "noteCount": 30,
  "noteAuthorCount": 30,
  "myRecord": {
    "status": "READING",
    "currentPage": 80,
    "myRating": 4.0,
    "lastRecordedAt": "2026-08-05T09:00:00Z"
  }
}
```

`myRecord`는 서재에 없으면 `null`이다. Figma의 `평점 100명 · 감상 30명`이 `raterCount`와
`noteAuthorCount`다.

## 6. 감상

### GET /books/{bookId}/notes

```
GET /books/bk_003/notes?sort=LATEST&scope=ALL&page=0&size=20
```

```json
{
  "totalCount": 30,
  "page": 0,
  "last": false,
  "notes": [
    {
      "id": "nt_2001",
      "bookId": "bk_003",
      "author": {
        "id": "rd_77",
        "anonymous": true,
        "displayName": "참새 1204"
      },
      "impression": "혼자 남겨진 사람이 절망 대신 계산기를 드는 이야기.",
      "quote": {
        "text": "나는 이 행성에서 과학으로 헤쳐 나갈 것이다.",
        "page": 80
      },
      "chapter": "Chapter 1",
      "readingPoint": 80,
      "readCompleted": true,
      "createdAt": "2026-08-05T10:00:00Z",
      "likeCount": 12,
      "likedByMe": false,
      "replies": [
        {
          "id": "rp_3001",
          "author": {
            "id": "rd_78",
            "anonymous": true,
            "displayName": "참새 0330"
          },
          "content": "감자 파트에서 진짜 웃었어요.",
          "createdAt": "2026-08-05T11:00:00Z",
          "likeCount": 3,
          "likedByMe": false
        }
      ]
    }
  ]
}
```

**중요**: `readingPoint`와 `quote.page`는 **서로 다른 값**이다.

- `readingPoint`: 어디까지 읽고 남긴 감상인가. **스포일러 판정 기준**이다
- `quote.page`: 인용한 문장이 몇 쪽에 있나

200쪽까지 읽고 50쪽 문장을 인용하면 `readingPoint: 200, quote.page: 50`이 된다.

`author.displayName`은 서버가 완성해서 내려준다. 익명이면 `참새 1204`, 실명이면 닉네임이다.
`(익명)` 접미사는 앱이 붙인다.

**스포일러 가림은 서버가 하지 않는다.** 서버는 전부 내려주고 앱이 `readingPoint`와 내 진행
쪽수를 비교해 가린다. 서버 가림으로 바꾸려면 별도 논의가 필요하다.

### POST /books/{bookId}/notes

```json
{
  "impression": "혼자 남겨진 사람이 절망 대신 계산기를 드는 이야기.",
  "quote": {
    "text": "나는 이 행성에서 과학으로 헤쳐 나갈 것이다.",
    "page": 80
  },
  "chapter": "Chapter 1",
  "readingPoint": 80
}
```

`impression`만 필수다. `quote`, `chapter`는 `null` 가능하다. 응답은 생성된 감상 객체(위와 동일한
모양)다.

### POST /notes/{noteId}/likes, DELETE /notes/{noteId}/likes

응답으로 갱신된 `likeCount`와 `likedByMe`를 내려준다.

```json
{ "likeCount": 13, "likedByMe": true }
```

### POST /notes/{noteId}/replies

```json
{ "content": "p.80 문장 좋네요. 담아갑니다." }
```

200자 이하. 응답은 생성된 답글 객체다.

### POST /replies/{replyId}/likes, DELETE /replies/{replyId}/likes

감상 좋아요와 같은 모양이다.

## 7. 내 서재

### GET /shelf

```
GET /shelf?status=READING&sort=RECENT
```

`status`가 없으면 전체다.

```json
{
  "totalCount": 12,
  "books": [
    {
      "book": {
        "id": "bk_003",
        "title": "마션",
        "authors": ["앤디 위어"],
        "category": "SF",
        "totalPages": 308,
        "coverUrl": "https://cdn.chaekchaek.app/covers/bk_003.png"
      },
      "status": "READING",
      "currentPage": 80,
      "myRating": 4.0,
      "lastRecordedAt": "2026-08-05T09:00:00Z"
    }
  ]
}
```

`totalCount`는 필터와 무관한 전체 권수다(Figma의 `전체 12권`).

**서버가 지켜야 할 불변식** (앱 도메인과 같다)

- `status == FINISHED` 이면 `currentPage == book.totalPages`
- `status == WANT_TO_READ` 이면 `currentPage == 0`

### PUT /shelf/{bookId}

서재에 없으면 추가하고, 있으면 상태를 바꾼다.

```json
{ "status": "READING" }
```

상태를 바꾸면 서버도 쪽수를 함께 조정한다.

| 요청 상태 | `currentPage` |
| --- | --- |
| `FINISHED` | `book.totalPages`로 설정 |
| `WANT_TO_READ` | `0`으로 설정 |
| `READING` | 유지 |

### PATCH /shelf/{bookId}/progress

```json
{ "currentPage": 160 }
```

**서재에 없는 책이면 `READING` 상태로 추가한다.** 앱의 「쪽수 기록 = 읽는 중」 규칙과 맞춘다.
`currentPage == book.totalPages`이면 상태를 `FINISHED`로 바꾼다.

### DELETE /shelf

여러 권을 한 번에 지운다(편집 모드).

```json
{ "bookIds": ["bk_003", "bk_004"] }
```

### PATCH /shelf/status

여러 권의 상태를 한 번에 바꾼다(편집 모드의 「상태 변경」).

```json
{ "bookIds": ["bk_003", "bk_004"], "status": "FINISHED" }
```

쪽수 조정 규칙은 `PUT /shelf/{bookId}`와 같다.

## 8. 별점

### PUT /books/{bookId}/rating

책당 하나이며 다시 매기면 덮어쓴다. 0.5 단위만 허용한다.

```json
{ "rating": 4.0 }
```

### GET /me/ratings/recent

별점 다이얼로그의 「내 평점 기록」이다. **이 책의 이력이 아니라 내가 최근에 매긴 별점 목록**이다.

```
GET /me/ratings/recent?limit=3
```

```json
{
  "ratings": [
    { "bookId": "bk_001", "title": "보이지 않는 도시", "rating": 3.5, "ratedAt": "2026-05-12" },
    { "bookId": "bk_002", "title": "역병", "rating": 4.0, "ratedAt": "2026-06-21" },
    { "bookId": "bk_003", "title": "마션", "rating": 4.0, "ratedAt": "2026-08-05" }
  ]
}
```

## 9. 프로필

### GET /me

```json
{
  "id": "rd_77",
  "nickname": "골똘한 참새",
  "publishesAnonymously": true,
  "anonymousHandle": "참새 1204"
}
```

`nickname`은 아직 정하지 않았으면 `null`이다.

### PATCH /me/profile

```json
{ "nickname": "골똘한 참새", "publishesAnonymously": false }
```

**서버가 거부해야 하는 조합**: `publishesAnonymously == false` 인데 저장된 닉네임도 없고 요청에도
`nickname`이 없는 경우. 앱도 같은 규칙을 `ReaderProfile` 생성자에서 막는다.

닉네임은 2~10자다.

## 10. 앱 도메인과의 매핑

| 응답 필드 | 앱 도메인 |
| --- | --- |
| `book.totalPages` | `PageCount` |
| `currentPage`, `readingPoint`, `quote.page` | `PageNumber` |
| `rating`, `myRating` | `Rating` (0.5 단위, 반개 정수로 보관) |
| `rating.average` | `RatingSummary.average` (0.5 단위 아님) |
| `author.displayName` + `anonymous` | `NoteAuthor.Named` / `NoteAuthor.Anonymous` |
| `status` | `ReadingStatus` |
| `sections[].type` | `FeedSection` 하위 타입 |
| `createdAt`, `lastRecordedAt` | `java.time.Instant` |
| `ratedAt` | `java.time.LocalDate` |
