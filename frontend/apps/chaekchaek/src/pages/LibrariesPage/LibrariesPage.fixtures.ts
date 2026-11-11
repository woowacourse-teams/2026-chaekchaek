export const createdLibraryResponse = {
  libraryId: 3,
  name: '여행하며 읽을 책',
  isDefault: false,
  totalCount: 0,
  items: [],
};

export const librariesResponse = {
  memberId: 3,
  libraries: [
    {
      libraryId: 1,
      name: '올해 읽을 책',
      isDefault: true,
      totalCount: 2,
      items: [
        {
          bookId: 42,
          title: '데미안',
          spineImageUrl: 'https://static.example.com/books/42/spine.webp',
        },
        {
          bookId: 43,
          title: '어린 왕자',
          spineImageUrl: 'https://static.example.com/books/43/spine.webp',
        },
      ],
    },
    {
      libraryId: 2,
      name: '다시 읽고 싶은 책',
      isDefault: false,
      totalCount: 1,
      items: [
        {
          bookId: 44,
          title: '마션',
          spineImageUrl: 'https://static.example.com/books/44/spine.webp',
        },
      ],
    },
  ],
};

export const librariesAfterCreateResponse = {
  ...librariesResponse,
  libraries: [...librariesResponse.libraries, createdLibraryResponse],
};

export const libraryToDelete = librariesResponse.libraries[1]!;
export const libraryToKeep = librariesResponse.libraries[0]!;

export const libraryToRename = librariesResponse.libraries[1]!;
export const renamedLibraryName = '주말에 읽을 책';

export const librariesAfterDeleteResponse = {
  ...librariesResponse,
  libraries: [libraryToKeep],
};

export const libraryToShare = librariesResponse.libraries[1]!;

export const libraryImageDataUrl =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=';

export const libraryToEditResponse = {
  ...librariesResponse.libraries[1]!,
  memberId: librariesResponse.memberId,
  nextPage: null,
  items: librariesResponse.libraries[1]!.items.map((book) => ({
    ...book,
    isbn13: '9788925588650',
  })),
};
