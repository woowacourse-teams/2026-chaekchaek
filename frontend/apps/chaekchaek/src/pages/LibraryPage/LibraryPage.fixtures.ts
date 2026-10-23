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
