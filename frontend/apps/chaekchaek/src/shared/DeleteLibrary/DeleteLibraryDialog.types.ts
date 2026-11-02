export type DeleteLibraryDialogProps = {
  libraryId: number;
  libraryName: string;
  onLibraryDeleted: () => Promise<void>;
  onClose: () => void;
};
