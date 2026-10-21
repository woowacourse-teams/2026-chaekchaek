import { Button, ButtonStack, Dialog } from '@chaekchaek/design-system';

import type { DeleteLibraryDialogProps } from './DeleteLibraryDialog.types';

export const DeleteLibraryDialog = ({ onClose }: DeleteLibraryDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle="‘나를 넓히는 책들’ 책장을 삭제합니다.">
          책장을 삭제할까요?
        </Dialog.Header>
        <Dialog.Footer>
          <ButtonStack>
            <Button block variant="ghost">
              취소
            </Button>
            <Button block variant="primary">
              책장 삭제
            </Button>
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
