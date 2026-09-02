export interface GetMembersMemberIdLibraryParams {
  memberId: number;
  page: number;
  status: 'ALL' | 'WANT_TO_READ' | 'READING' | 'FINISHED';
  sort: string;
}

export type GetMembersMemberIdLibrary = (params: GetMembersMemberIdLibraryParams) => Promise<{
  filteredCount: number;
  nextPage: number;
  totalCount: number;
  items: {
    coverImageUrl: string;
    rating: number;
    title: string;
    commentCount: number;
    bookId: number;
    translators: string[];
    isbn13: string;
    totalPages: number;
    publisher: string;
    publishedDate: string;
    currentPage: number;
    category: string;
    status: string;
    authors: string[];
  }[];
}>;
