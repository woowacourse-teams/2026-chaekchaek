export type BookInfoProps = {
  readingStatus: string | undefined;
  currentPage: number | undefined;
  totalPages: number | undefined;
  category: string | undefined;
  publishedDate: string | undefined;
  isbn13: string | undefined;
  authors: readonly unknown[] | undefined;
  translators: readonly unknown[] | undefined;
  onRatingCreate: () => void;
  onReadingStatusChange: (readingStatus: string) => void;
  onCurrentPageUpdate: () => void;
};
