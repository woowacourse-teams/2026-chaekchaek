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

export const renamedLibraryName = '다시 읽고 싶은 책';

export const searchedBook = {
  bookId: 44,
  title: '마션',
  isbn13: '9788925588650',
  authors: ['앤디 위어'],
  translators: ['박아람'],
  publisher: '알에이치코리아',
  publishedDate: '2021-05-04',
  category: '소설',
  coverImageUrl: 'https://static.example.com/books/44/cover.webp',
  spineImageUrl: 'https://static.example.com/books/44/spine.webp',
  totalPages: 600,
  reviewCount: 0,
  replyCount: 0,
  isRegisteredInMyLibrary: false,
};

export const bookSearchResponse = {
  totalCount: 1,
  nextPage: null,
  items: [searchedBook],
};

export const emptyBookSearchResponse = { totalCount: 0, nextPage: null, items: [] };

export const addedBookResponse = {
  ...searchedBook,
  addedAt: '2026-10-23T09:00:00',
  readingUpdatedAt: '2026-10-23T09:00:00',
  currentPage: 0,
  status: 'WANT_TO_READ',
  commentCount: 0,
  rating: null,
  backImageUrl: null,
};

export const libraryAfterAddResponse = {
  ...libraryResponse,
  totalCount: libraryResponse.totalCount + 1,
  items: [...libraryResponse.items, addedBookResponse],
};

export const downloadedLibraryImageDataUrl =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aCWQAAAAASUVORK5CYII=';

export const libraryBooks = Array.from({ length: 23 }, (_, index) => ({
  bookId: index + 100,
  title: `책장 도서 ${index + 1}`,
  spineImageUrl: `https://static.example.com/books/${index + 100}/spine.webp`,
  isbn13: String(9780000000000 + index),
  totalPages: 200,
}));

export const fullLibraryResponse = {
  libraryId: libraryResponse.libraryId,
  memberId: libraryResponse.memberId,
  name: libraryResponse.name,
  isDefault: libraryResponse.isDefault,
  totalCount: libraryBooks.length,
  items: libraryBooks,
};
