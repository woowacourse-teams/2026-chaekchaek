export type ManageLibraryDialogType =
  'ShareLibraryImageDialog' | 'RenameLibraryDialog' | 'DeleteLibraryDialog';

export type ManageLibraryDialogProps = {
  libraryName: string;
  onOpenDialog: (dialog: ManageLibraryDialogType) => void;
  onClose: () => void;
  onStartEdit: () => void;
};
