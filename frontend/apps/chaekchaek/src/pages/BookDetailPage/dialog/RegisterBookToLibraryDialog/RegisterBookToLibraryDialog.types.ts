export type RegisterBookToLibraryDialogProps = {
  book: { isbn: string; bookId?: number | undefined };
  onBookRegistered?: () => void | Promise<void>;
  onClose: () => void;
};
