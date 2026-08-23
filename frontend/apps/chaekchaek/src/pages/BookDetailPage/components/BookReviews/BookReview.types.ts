import type { BookReview } from './BookReviews.types';

export type BookReviewProps = {
  review: BookReview;
  onReviewsRefresh: () => void;
};
