export const endPoint = {
  '/api/v1/books': {
    get: {
      tags: ['도서'],
      summary: '도서 검색',
      description: '도서명과 페이지 번호로 도서를 검색한다',
      operationId: 'book-search',
      parameters: [
        {
          name: 'query',
          in: 'query',
          description: '검색할 도서명',
          required: true,
          schema: {
            type: 'string',
          },
        },
        {
          name: 'page',
          in: 'query',
          description: '1부터 시작하는 페이지 번호',
          required: true,
          schema: {
            type: 'integer',
            format: 'int32',
          },
        },
      ],
      responses: {
        200: {
          description: '200',
          content: {
            'application/json': {
              schema: {
                required: ['items', 'totalCount'],
                type: 'object',
                properties: {
                  nextPage: {
                    type: 'number',
                    description: '다음 페이지 번호. 마지막 페이지라면 null',
                    nullable: true,
                  },
                  totalCount: {
                    type: 'number',
                    description: '검색 결과의 전체 도서 수',
                  },
                  items: {
                    type: 'array',
                    description: '검색된 도서 목록. 한 페이지당 최대 10개',
                    items: {
                      required: [
                        'authors',
                        'category',
                        'coverImageUrl',
                        'isbn13',
                        'publishedDate',
                        'publisher',
                        'title',
                        'translators',
                      ],
                      type: 'object',
                      properties: {
                        translators: {
                          type: 'array',
                          description: '옮긴이 이름 목록',
                          items: {
                            type: 'string',
                          },
                        },
                        coverImageUrl: {
                          type: 'string',
                          description: '표지 이미지 URL. 알라딘 Big 규격(너비 200px)을 따름',
                        },
                        isbn13: {
                          type: 'string',
                          description: 'ISBN-13',
                        },
                        publisher: {
                          type: 'string',
                          description: '출판사',
                        },
                        publishedDate: {
                          type: 'string',
                          description: '출판일',
                        },
                        category: {
                          type: 'string',
                          description: '도서 카테고리',
                        },
                        title: {
                          type: 'string',
                          description: '도서 제목',
                        },
                        authors: {
                          type: 'array',
                          description: '저자 이름 목록',
                          items: {
                            type: 'string',
                          },
                        },
                      },
                    },
                  },
                },
              },
              examples: {
                'book-search': {
                  value:
                    '{"totalCount":1,"nextPage":null,"items":[{"title":"마션","coverImageUrl":"https://image.aladin.co.kr/martian.jpg","authors":["앤디 위어"],"translators":["박아람"],"publishedDate":"2026-07-01","isbn13":"9788925568683","category":"국내도서>소설>과학소설","publisher":"알에이치코리아(RHK)"}]}',
                },
              },
            },
          },
        },
        400: {
          description: '400',
          content: {
            'application/problem+json': {
              schema: {
                $ref: '#/components/schemas/ProblemDetail',
              },
              examples: {
                'book-search-invalid-request': {
                  value:
                    '{"detail":"요청값이 올바르지 않습니다.","instance":"/api/v1/books","status":400,"title":"Bad Request","type":"about:blank","code":"INVALID_REQUEST"}',
                },
              },
            },
          },
        },
        500: {
          description: '500',
          content: {
            'application/problem+json': {
              schema: {
                $ref: '#/components/schemas/ProblemDetail',
              },
              examples: {
                'book-search-internal-server-error': {
                  value:
                    '{"detail":"서버 내부 오류가 발생했습니다.","instance":"/api/v1/books","status":500,"title":"Internal Server Error","type":"about:blank","code":"INTERNAL_SERVER_ERROR"}',
                },
              },
            },
          },
        },
        502: {
          description: '502',
          content: {
            'application/problem+json': {
              schema: {
                $ref: '#/components/schemas/ProblemDetail',
              },
              examples: {
                'book-search-external-api-error': {
                  value:
                    '{"detail":"외부 서비스 호출에 실패했습니다.","instance":"/api/v1/books","status":502,"title":"Bad Gateway","type":"about:blank","code":"EXTERNAL_API_ERROR"}',
                },
              },
            },
          },
        },
      },
    },
  },
};
