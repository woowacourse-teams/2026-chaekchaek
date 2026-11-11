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
    },
    {
      bookId: 43,
      title: '어린 왕자',
      spineImageUrl: 'https://static.example.com/books/43/spine.webp',
      isbn13: '9788932917245',
    },
  ],
};

export const renamedLibraryName = '다시 읽고 싶은 책';

export const otherLibraryResponse = {
  ...libraryResponse,
  libraryId: 2,
  name: '주말에 읽을 책',
  isDefault: false,
};

export const librariesBeforeRenameResponse = {
  memberId: libraryResponse.memberId,
  libraries: [libraryResponse, otherLibraryResponse],
};

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
  commentCount: 0,
  rating: null,
  backImageUrl: null,
};

export const libraryAfterAddResponse = {
  ...libraryResponse,
  totalCount: libraryResponse.totalCount + 1,
  items: [...libraryResponse.items, addedBookResponse],
};

// 내 서재에는 있지만 현재 책장에는 아직 담기지 않은 책을 포함한다.
export const myLibraryResponse = {
  memberId: libraryResponse.memberId,
  libraries: [
    libraryResponse,
    {
      libraryId: 2,
      name: '내 서재',
      isDefault: false,
      totalCount: 1,
      items: [searchedBook],
    },
  ],
};

export const downloadedLibraryImageDataUrl =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aCWQAAAAASUVORK5CYII=';

export const libraryBooks = Array.from({ length: 23 }, (_, index) => ({
  bookId: index + 100,
  title: `책장 도서 ${index + 1}`,
  spineImageUrl: `https://static.example.com/books/${index + 100}/spine.webp`,
  isbn13: String(9780000000000 + index),
}));

export const fullLibraryResponse = {
  libraryId: libraryResponse.libraryId,
  memberId: libraryResponse.memberId,
  name: libraryResponse.name,
  isDefault: libraryResponse.isDefault,
  totalCount: libraryBooks.length,
  items: libraryBooks,
};

export const deletableLibraryResponse = {
  ...libraryResponse,
  libraryId: 3,
  name: '삭제할 책장',
  isDefault: false,
};

export const librariesBeforeDeleteResponse = {
  memberId: libraryResponse.memberId,
  libraries: [libraryResponse, deletableLibraryResponse],
};

export const librariesAfterDeleteResponse = {
  memberId: libraryResponse.memberId,
  libraries: [libraryResponse],
};

export const [bookToDelete, bookToKeep] = libraryResponse.items as [
  (typeof libraryResponse.items)[number],
  (typeof libraryResponse.items)[number],
];

export const libraryAfterBookDeleteResponse = {
  ...libraryResponse,
  totalCount: 1,
  items: [bookToKeep],
};
