export type Props = {
  bookId: number;
  currentPage: number;
  onCurrentPageUpdated: (...rest: unknown[]) => void | Promise<void>;
  onClose: () => void;
};
