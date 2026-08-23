export type WriteReplyProps = {
  reviewId: number;
  onReplyWritten: () => void | Promise<void>;
};
