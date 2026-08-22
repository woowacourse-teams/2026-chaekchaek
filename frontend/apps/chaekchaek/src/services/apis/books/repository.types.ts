export interface GetBooksParams {
  query: string;
  page: number;
}

export type GetBooks = (params: GetBooksParams) => Promise<{
  nextPage: number;
  totalCount: number;
  items: {
    translators: string[];
    coverImageUrl: string;
    isbn13: string;
    publisher: string;
    publishedDate: string;
    category: string;
    title: string;
    reviewCount: number | null;
    replyCount: number | null;
    authors: string[];
    bookId: number;
  }[];
}>;
