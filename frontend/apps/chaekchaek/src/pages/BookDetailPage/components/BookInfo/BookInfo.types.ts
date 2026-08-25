type MyRecord = {
  myRating: number;
  currentPage: number;
  status: string;
};

export type BookInfoProps = {
  myRecord: MyRecord | null;
  readingStatus: string | undefined;
  currentPage: number | undefined;
  totalPages: number | undefined;
  category: string | undefined;
  publishedDate: string | undefined;
  isbn13: string | undefined;
  authors: readonly unknown[] | undefined;
  translators: readonly unknown[] | undefined;
  onRegistryLibrary: () => void;
  onRatingCreate: () => void;
  onReadingStatusChange: (readingStatus: string) => void;
  onCurrentPageUpdate: () => void;
};
