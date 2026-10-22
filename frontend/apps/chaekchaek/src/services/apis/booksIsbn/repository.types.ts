export interface GetBooksIsbnParams {
  isbn: string;
}

export type GetBooksIsbn = (params: GetBooksIsbnParams) => Promise<{
  myRecord: {
    myRating: number;
    currentPage: number;
    status: string;
  };
  coverImageUrl: string;
  ratingCount: number;
  title: string;
  bookId: number;
  translators: undefined[];
  averageRating: number;
  isbn13: string;
  totalPages: number;
  publisher: string;
  publishedDate: string;
  category: string;
  authors: undefined[];
  description: string;
  reviewCount: number;
  replyCount: number;
  myRatingCount: number;
  bookQuestion: {
    question: string | null;
    plot: string | null;
    extract: string | null;
  } | null;
}>;
