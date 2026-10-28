import type { LibraryPageDialogType } from '../../LibraryPage';

export type ManageLibraryDialogProps = {
  onOpenDialog: (dialog: LibraryPageDialogType) => void;
  onClose: () => void;
};
