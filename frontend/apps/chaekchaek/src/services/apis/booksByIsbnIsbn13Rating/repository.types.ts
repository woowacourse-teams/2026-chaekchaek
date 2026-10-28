export interface PutBooksByIsbnIsbn13RatingCommand {
  isbn13: string;
  rating: number;
}

export type PutBooksByIsbnIsbn13Rating = (command: PutBooksByIsbnIsbn13RatingCommand) => Promise<{
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
  spineImageUrl: string;
  publishedDate: string;
  currentPage: number;
  category: string;
  backImageUrl: string;
  status: string;
  authors: string[];
}>;
export interface DeleteBooksByIsbnIsbn13RatingParams {
  isbn13: string;
}

export type DeleteBooksByIsbnIsbn13Rating = (
  params: DeleteBooksByIsbnIsbn13RatingParams,
) => Promise<undefined>;
