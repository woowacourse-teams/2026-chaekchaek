import type { BookReview } from './BookReviews.types';

export type BookReviewProps = {
  isbn: string;
  bookId: number | undefined;
  myRating: number | undefined;
  review: BookReview;
  onReviewsRefresh: () => void;
};
