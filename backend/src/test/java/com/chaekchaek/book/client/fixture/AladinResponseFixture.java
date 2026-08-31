package com.chaekchaek.book.client.fixture;

public final class AladinResponseFixture {

    private AladinResponseFixture() {
    }

    public static String 빈_검색_결과() {
        return """
                {
                  "totalResults": 0,
                  "startIndex": 2,
                  "itemsPerPage": 10,
                  "item": []
                }
                """;
    }

    public static String 마션_검색_결과() {
        return """
                {
                  "version": "20131101",
                  "title": "알라딘 검색결과 - 마션",
                  "totalResults": 6,
                  "startIndex": 1,
                  "itemsPerPage": 10,
                  "query": "마션",
                  "item": [
                    {
                      "title": "마션 (알라딘 리커버 특별판)",
                      "link": "https://www.aladin.co.kr/books/1",
                      "author": "앤디 위어 (지은이), 박아람 (옮긴이)",
                      "pubDate": "2026-07-01",
                      "description": "책 설명",
                      "isbn": "8925568683",
                      "isbn13": "9788925568683",
                      "itemId": 396798422,
                      "cover": "https://image.aladin.co.kr/martian.jpg",
                      "categoryName": "국내도서>소설>과학소설",
                      "publisher": "알에이치코리아(RHK)",
                      "seriesInfo": {
                        "seriesId": 1364731,
                        "seriesName": "앤디 위어 우주 3부작"
                      },
                      "subInfo": {}
                    }
                  ]
                }
                """;
    }

    public static String 인증키_오류() {
        return """
                {
                  "errorCode": 1,
                  "errorMessage": "잘못된 인증키입니다."
                }
                """;
    }

    public static String 다음_페이지가_있는_검색_결과() {
        return """
                {
                  "totalResults": 21,
                  "startIndex": 2,
                  "itemsPerPage": 10,
                  "item": []
                }
                """;
    }
}
