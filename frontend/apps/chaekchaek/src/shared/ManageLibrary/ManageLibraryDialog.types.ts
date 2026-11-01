import type { LibraryPageDialogType } from '../../pages/LibraryPage/LibraryPage';

export type ManageLibraryDialogProps = {
  onOpenDialog: (dialog: LibraryPageDialogType) => void;
  onClose: () => void;
  onStartEdit: () => void;
};
