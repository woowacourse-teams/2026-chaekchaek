export type RegisterBookToLibraryDialogProps = {
  book: { isbn: string; bookId?: number | undefined };
  onClose: () => void;
};
