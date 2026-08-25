export type ReadingStatus = 'WANT_TO_READ' | 'READING' | 'FINISHED';

export type RegisterLibraryDialogProps = {
  isbn: string;
  onLibraryRegistered: () => void;
  onClose: () => void;
};
