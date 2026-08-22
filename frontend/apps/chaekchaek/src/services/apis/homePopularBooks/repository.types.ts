export interface GetHomePopularBooksParams {}

export type GetHomePopularBooks = (params: GetHomePopularBooksParams) => Promise<{
  books: {
    replyCount: number;
    reviewCount: number;
    coverImageUrl: string;
    isbn13: string;
    title: string;
    authors: string[];
    bookId: number;
  }[];
}>;
