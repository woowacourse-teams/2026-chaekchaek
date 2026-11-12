export interface GetLibraryParams {
  page: number;
  sort: string;
}

export type GetLibrary = (params: GetLibraryParams) => Promise<{
  totalCount: number;
  filteredCount: number;
  nextPage: number;
  items: {
    bookId: number;
    isbn13: string;
    title: string;
    coverImageUrl: string;
    spineImageUrl: string | null;
    backImageUrl: string;
    authors: string[];
    translators: string[];
    publisher: string;
    category: string;
    publishedDate: string;
    commentCount: number;
    rating: number;
    addedAt: string;
  }[];
}>;
export interface PostLibraryCommand {
  isbn13: string;
}

export type PostLibrary = (command: PostLibraryCommand) => Promise<undefined>;
