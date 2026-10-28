export const bookNotInLibraryResponse = {
  bookId: 47,
  isbn13: '9788925588650',
  title: '마션',
  authors: ['앤디 위어'],
  translators: ['박아람'],
  publisher: '알에이치코리아',
  publishedDate: '2021-05-04',
  category: '소설',
  description: '화성에 홀로 남은 우주비행사의 생존 이야기',
  coverImageUrl: 'https://static.example.com/books/47/cover.jpg',
  totalPages: 600,
  averageRating: 0,
  ratingCount: 0,
  myRatingCount: 0,
  reviewCount: 0,
  replyCount: 0,
  isRegisteredInMyLibrary: false,
  myRecord: null,
};

export const bookInLibraryResponse = {
  ...bookNotInLibraryResponse,
  isRegisteredInMyLibrary: true,
  myRecord: {
    status: 'WANT_TO_READ',
    currentPage: 0,
    myRating: 0,
  },
};

export const emptyBookReviewsResponse = {
  totalCount: 0,
  nextPage: null,
  items: [],
};

// RegisterBookToLibraryDialog 테스트용 fixture: 책장 추가·제거·선택 유지
export const bookToRegister = bookNotInLibraryResponse;

export const unselectedLibraryResponse = {
  libraryId: 2,
  memberId: 3,
  name: '다시 읽고 싶은 책',
  isDefault: false,
  totalCount: 0,
  items: [],
};

export const librariesBeforeBookRegistrationResponse = {
  memberId: unselectedLibraryResponse.memberId,
  libraries: [unselectedLibraryResponse],
};

export const registeredBookResponse = {
  ...bookToRegister,
  spineImageUrl: 'https://static.example.com/books/47/spine.webp',
  currentPage: 0,
  status: 'WANT_TO_READ',
  addedAt: '2026-10-27T09:00:00',
  readingUpdatedAt: '2026-10-27T09:00:00',
};

export const bookToRemove = {
  ...registeredBookResponse,
  isRegisteredInMyLibrary: true,
};

export const selectedLibraryResponse = {
  ...unselectedLibraryResponse,
  totalCount: 1,
  items: [bookToRemove],
};

const defaultLibraryForBookRemoval = {
  ...selectedLibraryResponse,
  libraryId: 1,
  name: '내 서재',
  isDefault: true,
};

export const librariesBeforeBookRemovalResponse = {
  memberId: selectedLibraryResponse.memberId,
  libraries: [defaultLibraryForBookRemoval, selectedLibraryResponse],
};

export const unselectedLibraryForUnchangedSelectionResponse = {
  ...unselectedLibraryResponse,
  libraryId: 3,
  name: '나중에 읽을 책',
};

export const unchangedBookSelectionLibrariesResponse = {
  memberId: selectedLibraryResponse.memberId,
  libraries: [selectedLibraryResponse, unselectedLibraryForUnchangedSelectionResponse],
};
// RegisterBookToLibraryDialog 테스트용 fixture 끝
