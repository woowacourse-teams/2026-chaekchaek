import type { BookReview } from './BookReviews.types';

export type BookReviewProps = {
  isbn: string;
  review: BookReview;
  onReviewsRefresh: () => void;
};
