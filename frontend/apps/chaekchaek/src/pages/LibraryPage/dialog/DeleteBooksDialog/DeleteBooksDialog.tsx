import { Button, ButtonStack, Dialog } from '@chaekchaek/design-system';

import type { DeleteBooksDialogProps } from './DeleteBooksDialog.types';

export const DeleteBooksDialog = ({ onClose }: DeleteBooksDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle="선택한 1권의 책을 서재에서 삭제할까?">책 삭제</Dialog.Header>

        <Dialog.Footer>
          <ButtonStack>
            <Button type="button" variant="ghost" size="large" block onClick={onClose}>
              취소
            </Button>
            <Button type="button" variant="primary" size="large" block>
              삭제
            </Button>
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
