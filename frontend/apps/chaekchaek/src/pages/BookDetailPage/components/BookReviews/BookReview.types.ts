import type { BookReview } from './BookReviews.types';

export type BookReviewProps = {
  review: BookReview;
  isSpoilerVisible: boolean;
  onReviewsRefresh: () => void;
};
