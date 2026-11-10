export type ManageLibraryDialogType =
  'ShareLibraryImageDialog' | 'RenameLibraryDialog' | 'DeleteLibraryDialog';

export type ManageLibraryDialogProps = {
  libraryName: string;
  canDeleteLibrary: boolean;
  onOpenDialog: (dialog: ManageLibraryDialogType) => void;
  onClose: () => void;
  onStartEdit: () => void;
};
