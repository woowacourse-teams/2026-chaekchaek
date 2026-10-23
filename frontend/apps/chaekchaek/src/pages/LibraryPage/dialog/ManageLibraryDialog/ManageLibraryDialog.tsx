import { Button, Dialog, Divider, Icon, Text } from '@chaekchaek/design-system';

import type { ManageLibraryDialogProps } from './ManageLibraryDialog.types';

export const ManageLibraryDialog = ({ onOpenDialog, onClose }: ManageLibraryDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>나를 넓히는 책들</Dialog.Header>
        <Dialog.Body>
          <Button
            block
            align="left"
            size="large"
            onClick={() => {
              onOpenDialog('RenameLibraryDialog');
            }}
          >
            <Icon.PencilIcon size="large" sx={{ mr: 3 }} />
            책장 이름 바꾸기
          </Button>
          <Divider sx={{ mt: 1, mb: 1 }} />
          <Button
            block
            align="left"
            size="large"
            onClick={() => {
              onClose();
            }}
          >
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
          <Button
            block
            align="left"
            size="large"
            onClick={() => {
              onOpenDialog('ShareLibraryImageDialog');
            }}
          >
            <Icon.DownloadIcon size="large" sx={{ mr: 3 }} />
            이미지 저장
          </Button>
          <Divider sx={{ mt: 1, mb: 1 }} />
          <Button
            block
            align="left"
            size="large"
            onClick={() => {
              onOpenDialog('DeleteLibraryDialog');
            }}
          >
            <Icon.TrashIcon color="error" sx={{ mr: 3 }} />
            <Text color="error">책장 삭제</Text>
          </Button>
          <Divider sx={{ mt: 1, mb: 1 }} />
        </Dialog.Body>
      </Dialog.Container>
    </Dialog>
  );
};
