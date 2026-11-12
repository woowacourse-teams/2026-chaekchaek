import type { BookReview } from '../../components/BookReviews';

export type UpdateReviewDialogProps = {
  isbn: string;
  bookId: number | undefined;
  myRating: number | undefined;
  review: BookReview;
  onReviewUpdated: () => void;
  onClose: () => void;
};
