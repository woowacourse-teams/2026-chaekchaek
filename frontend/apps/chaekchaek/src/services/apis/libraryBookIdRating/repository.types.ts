export interface PutLibraryBookIdRatingCommand {
  bookId: number;
  rating: number;
}

export type PutLibraryBookIdRating = (command: PutLibraryBookIdRatingCommand) => Promise<{
  addedAt: string;
  coverImageUrl: string;
  rating: number;
  title: string;
  readingUpdatedAt: string;
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
}>;
export interface DeleteLibraryBookIdRatingParams {
  bookId: number;
}

export type DeleteLibraryBookIdRating = (
  params: DeleteLibraryBookIdRatingParams,
) => Promise<undefined>;
