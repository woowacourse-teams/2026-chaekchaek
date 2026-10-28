import type { BookReview } from '../../components/BookReviews';

export type UpdateReviewDialogProps = {
  bookId: number | undefined;
  myRating: number | undefined;
  review: BookReview;
  onReviewUpdated: () => void;
  onClose: () => void;
};
