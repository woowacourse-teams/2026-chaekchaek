export type DeleteBooksDialogProps = {
  bookIds: number[];
  onBooksDeleted: () => Promise<void> | void;
  onClose: () => void;
};
