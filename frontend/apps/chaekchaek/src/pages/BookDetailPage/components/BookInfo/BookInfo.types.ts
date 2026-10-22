type MyRecord = {
  myRating: number;
  status: string;
};

export type BookInfoProps = {
  myRecord: MyRecord | null;
  readingStatus: string | undefined;
  category: string | undefined;
  publishedDate: string | undefined;
  isbn13: string | undefined;
  authors: readonly unknown[] | undefined;
  translators: readonly unknown[] | undefined;
  purchaseUrl: string | null | undefined;
  onRegistryLibrary: () => void;
  onRatingCreate: () => void;
  onReadingStatusChange: (readingStatus: string) => void;
};
