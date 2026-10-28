export interface PostBooksByIsbnIsbn13ReviewsCommand {
  isbn13: string;
  isSpoiler?: boolean | undefined;
  quote?: string | undefined;
  totalPages?: number | undefined;
  content: string;
}

type RequestContext = {
  guestToken: string;
};

export type PostBooksByIsbnIsbn13Reviews = (
  command: PostBooksByIsbnIsbn13ReviewsCommand,
  context?: RequestContext,
) => Promise<undefined>;
