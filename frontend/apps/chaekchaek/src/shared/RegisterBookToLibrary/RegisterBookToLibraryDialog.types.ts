export type RegisterBookToLibraryDialogProps = {
  book: { isbn: string; bookId?: number | undefined };
  onBookRegistered?: (changed: { added: boolean; removed: boolean }) => void | Promise<void>;
  onClose: () => void;
};
