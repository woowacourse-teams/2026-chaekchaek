export type ManageLibraryDialogType =
  'ShareLibraryImageDialog' | 'RenameLibraryDialog' | 'DeleteLibraryDialog';

export type ManageLibraryDialogProps = {
  onOpenDialog: (dialog: ManageLibraryDialogType) => void;
  onClose: () => void;
  onStartEdit: () => void;
};
