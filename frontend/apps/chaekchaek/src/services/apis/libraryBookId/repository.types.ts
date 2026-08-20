export interface DeleteLibraryBookIdParams {
  bookId: number;
}

export type DeleteLibraryBookId = (params: DeleteLibraryBookIdParams) => Promise<undefined>;
export interface PatchLibraryBookIdParams {
  bookId: number;
  status?: string;
  currentPage?: number;
}

export type PatchLibraryBookId = (params: PatchLibraryBookIdParams) => Promise<{
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
}>;
