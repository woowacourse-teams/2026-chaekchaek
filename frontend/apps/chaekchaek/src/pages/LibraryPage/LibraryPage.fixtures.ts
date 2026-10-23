export const libraryResponse = {
  libraryId: 1,
  memberId: 3,
  name: '올해 읽을 책',
  isDefault: true,
  totalCount: 2,
  nextPage: null,
  items: [
    {
      bookId: 42,
      title: '데미안',
      spineImageUrl: 'https://static.example.com/books/42/spine.webp',
      isbn13: '9788937460449',
      totalPages: 239,
    },
    {
      bookId: 43,
      title: '어린 왕자',
      spineImageUrl: 'https://static.example.com/books/43/spine.webp',
      isbn13: '9788932917245',
      totalPages: 136,
    },
  ],
};

// 10권씩 반환되는 API의 모든 페이지를 조회해야 전체 책등을 표시할 수 있다.
export const paginatedLibraryBooks = Array.from({ length: 23 }, (_, index) => ({
  bookId: index + 100,
  title: `책장 도서 ${index + 1}`,
  spineImageUrl: `https://static.example.com/books/${index + 100}/spine.webp`,
  isbn13: String(9780000000000 + index),
  totalPages: 200,
}));

export const libraryPageResponses = [
  {
    ...libraryResponse,
    totalCount: paginatedLibraryBooks.length,
    nextPage: 2,
    items: paginatedLibraryBooks.slice(0, 10),
  },
  {
    ...libraryResponse,
    totalCount: paginatedLibraryBooks.length,
    nextPage: 3,
    items: paginatedLibraryBooks.slice(10, 20),
  },
  {
    ...libraryResponse,
    totalCount: paginatedLibraryBooks.length,
    nextPage: null,
    items: paginatedLibraryBooks.slice(20),
  },
];
