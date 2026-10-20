export type UpdateBookStatusDialogProps = {
  bookSelection: number[];
  onBookStatusUpdated: () => Promise<void> | void;
  onClose: () => void;
};
