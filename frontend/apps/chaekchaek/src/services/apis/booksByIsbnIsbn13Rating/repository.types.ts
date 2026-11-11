export interface PutBooksByIsbnIsbn13RatingCommand {
  isbn13: string;
  rating: number;
}

export type PutBooksByIsbnIsbn13Rating = (command: PutBooksByIsbnIsbn13RatingCommand) => Promise<{
  addedAt: string;
  coverImageUrl: string;
  rating: number;
  title: string;
  commentCount: number;
  bookId: number;
  translators: string[];
  isbn13: string;
  publisher: string;
  spineImageUrl: string;
  publishedDate: string;
  category: string;
  backImageUrl: string;
  authors: string[];
}>;
export interface DeleteBooksByIsbnIsbn13RatingParams {
  isbn13: string;
}

export type DeleteBooksByIsbnIsbn13Rating = (
  params: DeleteBooksByIsbnIsbn13RatingParams,
) => Promise<undefined>;
