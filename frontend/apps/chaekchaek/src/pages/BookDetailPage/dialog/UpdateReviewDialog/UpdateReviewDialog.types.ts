import type { BookReview } from '../../components/BookReviews';

export type UpdateReviewDialogProps = {
  review: BookReview;
  onReviewUpdated: () => void;
  onClose: () => void;
};
