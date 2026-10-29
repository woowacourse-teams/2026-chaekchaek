export type AddBooksToLibraryDialogProps = {
  libraryId: number;
  onBookAdded: () => Promise<void>;
  onClose: () => void;
};
