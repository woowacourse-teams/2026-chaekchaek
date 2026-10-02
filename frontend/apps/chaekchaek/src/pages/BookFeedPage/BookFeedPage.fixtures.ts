// /api/v1/feed/reviews 예제를 바탕으로 한 스포일러 없는 감상 목록
export const feedReviewsPage1 = {
  totalCount: 3,
  nextPage: 2,
  reviews: [
    {
      reviewId: 123,
      content: '감상 내용',
      isSpoiler: false,
      createdAt: '2026-09-28T10:00:00Z',
      author: {
        memberId: 1,
        displayName: '독자',
        profileImageUrl: null,
        anonymous: false,
        mine: false,
        actorType: 'MEMBER',
        profileStatus: 'AVAILABLE',
      },
      replyCount: 3,
      bookId: 42,
      isbn13: '9788936433598',
      bookTitle: '도서 제목',
      bookCoverImageUrl: 'https://example.com/cover.jpg',
    },
    {
      reviewId: 122,
      content: '책을 덮고도 오래 생각하게 되는 이야기였다.',
      isSpoiler: false,
      createdAt: '2026-09-27T10:00:00Z',
      author: {
        memberId: 2,
        displayName: '다정한 참새',
        profileImageUrl: null,
        anonymous: false,
        mine: false,
        actorType: 'MEMBER',
        profileStatus: 'AVAILABLE',
      },
      replyCount: 5,
      bookId: 43,
      isbn13: '9788925568683',
      bookTitle: '마션',
      bookCoverImageUrl: 'https://example.com/martian.jpg',
    },
  ],
};

export const feedReviewsPage2 = {
  totalCount: 3,
  nextPage: null,
  reviews: [
    {
      reviewId: 121,
      content: '도시는 기억으로 만들어진다는 문장에서 오래 멈췄다.',
      isSpoiler: false,
      createdAt: '2026-09-26T10:00:00Z',
      author: {
        memberId: 3,
        displayName: '책 읽는 고양이',
        profileImageUrl: null,
        anonymous: false,
        mine: false,
        actorType: 'MEMBER',
        profileStatus: 'AVAILABLE',
      },
      replyCount: 2,
      bookId: 44,
      isbn13: '9788937441726',
      bookTitle: '보이지 않는 도시들',
      bookCoverImageUrl: 'https://example.com/invisible-cities.jpg',
    },
  ],
};
