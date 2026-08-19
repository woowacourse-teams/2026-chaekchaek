export interface GetLibraryParams {
  page: number;
  status: string;
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
    authors: string[];
    translators: string[];
    publisher: string;
    category: string;
    publishedDate: string;
    totalPages: number;
    commentCount: number;
    status: string;
    currentPage: number;
    rating: number;
    addedAt: string;
    readingUpdatedAt: string;
  }[];
}>;
export interface PostLibraryCommand {
  isbn13: string;
  totalPages?: number;
  status: string;
}

export type PostLibrary = (command: PostLibraryCommand) => Promise<undefined>;
