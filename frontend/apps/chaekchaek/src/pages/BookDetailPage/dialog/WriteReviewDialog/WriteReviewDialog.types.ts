export type WriteReviewDialogProps = {
  bookId: number;
  isbn: string;
  onReviewWritten: () => void;
  onClose: () => void;
};
