import { Button, Dialog, Divider, Icon, Text } from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import type { ManageLibraryDialogProps } from './ManageLibraryDialog.types';

export const ManageLibraryDialog = ({
  libraryName,
  canDeleteLibrary,
  onStartEdit,
  onOpenDialog,
  onClose,
}: ManageLibraryDialogProps) => {
  const handleClickRenameLibrary = () => {
    onOpenDialog('RenameLibraryDialog');
    track('library_rename_open');
  };
  const handleClickShareLibraryImage = () => {
    onOpenDialog('ShareLibraryImageDialog');
    track('library_save_image_open');
  };
  const handleClickStartEdit = () => {
    onStartEdit();
    onClose();
    track('library_start_edit');
  };

  const handleClickDeleteLibrary = () => {
    onOpenDialog('DeleteLibraryDialog');
    track('library_delete_open');
  };
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>{libraryName}</Dialog.Header>
        <Dialog.Body>
          <Button block align="left" size="large" onClick={handleClickRenameLibrary}>
            <Icon.PencilIcon size="large" sx={{ mr: 3 }} />
            책장 이름 바꾸기
          </Button>
          <Divider sx={{ mt: 1, mb: 1 }} />
          <Button block align="left" size="large" onClick={handleClickStartEdit}>
            <Icon.DragHandleIcon size="large" sx={{ mr: 3 }} />책 삭제
          </Button>
          <Divider sx={{ mt: 1, mb: 1 }} />
          {/* <Button
            block
            align="left"
            size="large"
            onClick={() => {
              onClose();
            }}
          >
            <Icon.UploadIcon size="large" sx={{ mr: 3 }} />
            책장 공유
          </Button>
          <Divider sx={{ mt: 1, mb: 1 }} /> */}
          <Button block align="left" size="large" onClick={handleClickShareLibraryImage}>
            <Icon.DownloadIcon size="large" sx={{ mr: 3 }} />
            이미지 저장
          </Button>
          {canDeleteLibrary && (
            <>
              <Divider sx={{ mt: 1, mb: 1 }} />
              <Button block align="left" size="large" onClick={handleClickDeleteLibrary}>
                <Icon.TrashIcon color="error" sx={{ mr: 3 }} />
                <Text color="error">책장 삭제</Text>
              </Button>
            </>
          )}
          <Divider sx={{ mt: 1, mb: 1 }} />
        </Dialog.Body>
      </Dialog.Container>
    </Dialog>
  );
};
