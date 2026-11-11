export interface GetMembersMemberIdLibraryParams {
  memberId: number;
  page: number;
  sort: string;
}

export type GetMembersMemberIdLibrary = (params: GetMembersMemberIdLibraryParams) => Promise<{
  member: {
    memberId: number;
    displayName: string;
    profileImageUrl: string;
  };
  filteredCount: number;
  nextPage: number;
  totalCount: number;
  items: {
    coverImageUrl: string;
    spineImageUrl: string;
    backImageUrl: string;
    rating: number;
    title: string;
    commentCount: number;
    bookId: number;
    translators: string[];
    isbn13: string;
    publisher: string;
    publishedDate: string;
    category: string;
    authors: string[];
  }[];
}>;
