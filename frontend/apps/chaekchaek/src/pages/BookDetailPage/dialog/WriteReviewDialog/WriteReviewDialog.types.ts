export type WriteReviewDialogProps = {
  bookId: number;
  isbn: string;
  myRating: number | undefined;
  onReviewWritten: () => void;
  onClose: () => void;
};
