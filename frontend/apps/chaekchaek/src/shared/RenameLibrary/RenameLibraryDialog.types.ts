export type RenameLibraryDialogProps = {
  libraryId: number;
  name: string;
  onLibraryRenamed: () => Promise<void>;
  onClose: () => void;
};
