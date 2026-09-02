package com.chaekchaek.book.client.fixture;

public final class Yes24ResponseFixture {

    private Yes24ResponseFixture() {
    }

    public static String 헤르만_헤세_검색_결과() {
        return 검색_결과(1, 10, 1);
    }

    public static String 검색_결과(int currentPage, int pageSize, int totalCount) {
        return """
                {
                  "success": true,
                  "message": "성공",
                  "errorCode": null,
                  "data": {
                    "meta": {
                      "apiTitle": "상품 검색",
                      "version": "v1"
                    },
                    "items": [{
                      "itemId": 101375809,
                      "title": "데미안",
                      "author": "헤르만 헤세 저/전영애 역",
                      "goodsSortNm": "국내도서",
                      "publisher": "민음사",
                      "isbn13": "9788937460449",
                      "publishDate": "20001220",
                      "cover": "https://image.yes24.com/goods/101375809/L"
                    }],
                    "currentPage": %d,
                    "pageSize": %d,
                    "totalCount": %d
                  }
                }
                """.formatted(currentPage, pageSize, totalCount);
    }

    public static String 데미안_상세_결과() {
        return """
                {
                  "success": true,
                  "message": "성공",
                  "errorCode": null,
                  "data": {
                    "meta": {
                      "apiTitle": "상품 상세 조회",
                      "version": "v1"
                    },
                    "items": [{
                      "itemId": 101375809,
                      "title": "데미안",
                      "author": "헤르만 헤세 저/전영애 역",
                      "goodsSortNm": "국내도서",
                      "publisher": "민음사",
                      "isbn13": "9788937460449",
                      "publishDate": "2000-12-20",
                      "pages": 240,
                      "cover": "https://image.yes24.com/goods/101375809/L",
                      "contentDetail": {
                        "bookIntroduction": "내면의 길을 찾아가는 성장 소설",
                        "bookSummary": null,
                        "tableOfContents": "두 세계"
                      }
                    }],
                    "currentPage": 1,
                    "pageSize": 20,
                    "totalCount": 1
                  }
                }
                """;
    }

    public static String 오류_응답(String errorCode, String message) {
        return """
                {
                  "success": false,
                  "errorCode": "%s",
                  "message": "%s",
                  "data": null
                }
                """.formatted(errorCode, message);
    }

    public static String 잘못된_출간일_검색_결과() {
        return 헤르만_헤세_검색_결과().replace("20001220", "not-a-date");
    }
}
