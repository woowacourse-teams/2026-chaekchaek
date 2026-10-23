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
