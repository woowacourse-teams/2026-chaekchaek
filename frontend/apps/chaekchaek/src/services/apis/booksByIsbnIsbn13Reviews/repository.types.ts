export interface PostBooksByIsbnIsbn13ReviewsCommand {
  isbn13: string;
  chapter?: string | undefined;
  isSpoiler?: boolean | undefined;
  quote?: string | undefined;
  totalPages?: number | undefined;
  currentPage?: number | undefined;
  content: string;
}

export type PostBooksByIsbnIsbn13Reviews = (
  command: PostBooksByIsbnIsbn13ReviewsCommand,
) => Promise<undefined>;
