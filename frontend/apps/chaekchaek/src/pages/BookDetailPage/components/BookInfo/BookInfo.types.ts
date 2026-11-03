export type BookInfoProps = {
  category: string | undefined;
  publishedDate: string | undefined;
  isbn13: string | undefined;
  authors: readonly unknown[] | undefined;
  translators: readonly unknown[] | undefined;
  isRegisteredInMyLibrary: boolean | undefined;
  onRegisterBookToLibrary: () => void;
  purchaseUrl: string | null | undefined;
};
