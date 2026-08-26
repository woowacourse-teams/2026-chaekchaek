# 구현 인수인계

최종 갱신: 2026-08-25

## 현재 작업

| 항목 | 값 |
| --- | --- |
| 브랜치 | `205-feat-android-todo-integration` |
| GitHub 이슈 | [#205 누락기능 수정](https://github.com/woowacourse-teams/2026-chaekchaek/issues/205) |
| Draft PR | [#206 누락 기능 수정](https://github.com/woowacourse-teams/2026-chaekchaek/pull/206) |
| 기준 브랜치 | `an-develop` |
| Pencil SSOT | `/Users/ujeonghyeon/Downloads/designs.pen` |

## 구현 완료

- 감상 스포일러 체크 기반 잠금
  - 감상, 발췌, 답글의 공백과 문장부호를 유지하고 실제 원문 길이만큼 `짹`으로 표시
  - 가려진 감상 탭 시 해당 감상 한 건만 즉시 공개
  - 감상 쪽수는 입력과 표시에만 사용하고 잠금 판정에서는 제외
- 검색 정렬
  - `LATEST`, `COMMENT`를 `GET /api/v1/books`의 `sort`로 전달
  - 선택 상태를 유지하고 현재 검색어로 즉시 재조회
- API 로딩
  - 홈, 검색, 상세 감상 요청이 500ms 안에 끝나면 표시하지 않음
  - 500ms가 지나면 표시하고 응답 즉시 닫음
- 감상과 답글
  - 전체 답글 페이지 조회와 중복 제거
  - 감상과 답글의 `likedByMe` 기반 반응 등록·취소
  - 답글 200자 제한과 글자 수 표시
- 도서 상세
  - 서재 등록 상태에 따라 추가·해제 전환
  - 최근 별점 3개 표시와 저장 직후 갱신
- 로그인과 감상 작성
  - Google 로그인 시트에 개인정보처리방침 링크 연결
  - 이용약관 링크는 제공하지 않음
  - 감상 1000자 수, 작성 취소 확인, 실제 익명 설정과 공개 닉네임 표시

## API 근거

- 검색 정렬 계약은 운영 API에서 `LATEST`, `COMMENT` 응답을 확인했다.
- 감상·답글·반응 경로는 백엔드 커밋 `22d15c5`의 `ReviewController.java`와 응답 DTO에서 확인했다.
- 개인정보처리방침은 다음 주소만 사용한다.
  - `https://app.notion.com/p/3b185850b3e18085b919d108ce7cd4ef?source=copy_link`

## 검증 결과

```bash
./gradlew :shared:allTests
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:lintDebug
```

위 명령은 모두 통과했다. Pixel 6a API 33 AVD를 콜드 부팅하고 디버그 APK 설치와 실행을 확인했다.
홈 렌더링, 검색 결과, `LATEST`에서 `COMMENT` 정렬 전환, 상세 화면, 비로그인 로그인 시트의
개인정보처리방침 노출을 실제 레이아웃에서 확인했다.

shared 테스트와 app lint를 한 Gradle 호출에 함께 넣으면 AGP 9.0.1과 KSP의
`generateAndroidHostTestLintModel` 암시적 의존성 검사로 실패한다. 각각 단독 실행하면 통과한다.

## 남은 작업

- PR #206은 Draft 상태다. 리뷰 준비가 끝나면 Ready로 전환한다.
- 열린 PR #202와 `BookDetailScreen.kt`, 검색 화면 파일이 겹쳐 커밋 훅의 통합 APK 병합 검증은
  충돌한다. 현재 브랜치 자체의 테스트, lint, APK 빌드는 통과한다.
- `BookDetailScreen.kt`는 화면 조립, API 상태, 인증, 입력 UI를 한 파일에서 담당한다.
  단일 책임 원칙에 맞춘 분리는 별도 리팩터링 이슈로 진행한다.
- `BookDetailRemoteRepository`도 책 상세, 서재, 감상, 답글 API를 함께 담당한다. API 경계가 더
  커질 때 저장소를 역할별로 분리한다.

## 작업 규칙

- Android 변경은 `/android` 안에서만 수행한다.
- 디자인 조회와 수정은 Pencil SSOT만 사용하고 저장소에 `.pen` 복사본을 두지 않는다.
- `an-develop`을 원격 최신 기준으로 유지하고 `main`에는 직접 push하거나 merge하지 않는다.
- 코드나 문서 변경은 검증 뒤 의미 단위 커밋으로 남긴다.
