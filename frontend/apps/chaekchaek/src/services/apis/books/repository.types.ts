export interface GetBooksParams {
  query: string;
  page: number;
  undefined;
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
    commentCount: number;
    authors: string[];
    bookId: number;
  }[];
}>;
