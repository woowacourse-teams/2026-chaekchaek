# 책췍 용어사전

## `totalCount`

외부 도서 공급자가 알려준 검색 결과 총 건수나 목록 응답의 전체 항목 수를 의미한다.

다음 응답에서 페이징 대상의 전체 건수를 나타낼 때 사용한다.

- 도서 검색 결과의 전체 도서 수
- 서재 목록의 전체 도서 수
- 감상 목록의 전체 감상 수

## `totalActivityCount`

특정 책에 연결된 감상 수와 답글 수의 합을 의미한다.

```text
totalActivityCount = reviewCount + replyCount
```

도서 검색 결과를 감상·답글 활동량으로 정렬하거나 책 단위 활동량을 계산할 때 사용한다.

`totalCount`와 달리 페이징 대상의 전체 건수가 아니므로, 활동 합계를 표현할 때는 `totalActivityCount`를 사용한다.

## 네이밍 원칙

- 하나의 필드명으로 서로 다른 개념을 표현하지 않는다.
- 페이징 대상의 전체 건수는 `totalCount`를 사용한다.
- 감상과 답글의 합계는 `totalActivityCount`를 사용한다.
