export type WriteReplyProps = {
  isbn: string;
  reviewId: number;
  onReplyWritten: () => void | Promise<void>;
};
