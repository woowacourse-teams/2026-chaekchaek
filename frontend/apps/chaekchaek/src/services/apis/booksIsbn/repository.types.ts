export interface GetBooksIsbnParams {
  isbn: string;
}

export type GetBooksIsbn = (params: GetBooksIsbnParams) => Promise<{
  myRecord: {
    myRating: number;
  };
  coverImageUrl: string;
  description: string;
  source: string;
  ratingCount: number;
  title: string;
  myRatingCount: number;
  bookId: number;
  purchaseUrl: string;
  replyCount: number;
  translators: undefined[];
  reviewCount: number;
  bookQuestion: {
    extract: string;
    question: string;
    plot: string;
  };
  averageRating: number;
  isbn13: string;
  totalPages: number;
  publisher: string;
  publishedDate: string;
  isRegisteredInMyLibrary: boolean;
  category: string;
  authors: undefined[];
}>;
