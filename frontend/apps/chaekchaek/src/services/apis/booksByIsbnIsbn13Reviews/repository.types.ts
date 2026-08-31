export interface PostBooksByIsbnIsbn13ReviewsCommand {
  isbn13: string;
  chapter: string;
  isSpoiler: boolean;
  quote: string;
  totalPages: number;
  currentPage: number;
  content: string;
}

export type PostBooksByIsbnIsbn13Reviews = (
  command: PostBooksByIsbnIsbn13ReviewsCommand,
) => Promise<undefined>;
