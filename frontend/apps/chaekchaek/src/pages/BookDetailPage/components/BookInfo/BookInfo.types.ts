export type BookInfoProps = {
  category: string | undefined;
  publishedDate: string | undefined;
  isbn13: string | undefined;
  authors: readonly unknown[] | undefined;
  translators: readonly unknown[] | undefined;
  purchaseUrl: string | null | undefined;
};
