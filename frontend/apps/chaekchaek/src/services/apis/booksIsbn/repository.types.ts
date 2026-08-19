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
  commentCount: number;
  bookId: number;
  translators: undefined[];
  averageRating: number;
  isbn13: string;
  totalPages: number;
  publisher: string;
  publishedDate: string;
  category: string;
  authors: undefined[];
}>;
