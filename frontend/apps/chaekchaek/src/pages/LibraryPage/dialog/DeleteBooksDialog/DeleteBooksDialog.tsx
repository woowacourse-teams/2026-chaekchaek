import { Button, ButtonStack, Dialog } from '@chaekchaek/design-system';

import { postLibraryBulkDelete } from '@/services/apis/libraryBulkDelete/repository';
import { useExecute } from '@/services/core/useExecute';

import type { DeleteBooksDialogProps } from './DeleteBooksDialog.types';

export const DeleteBooksDialog = ({ bookIds, onClose }: DeleteBooksDialogProps) => {
  const { mutate: deleteBooks } = useExecute({
    executeFn: postLibraryBulkDelete,
  });

  const handleDeleteBooks = async () => {
    await deleteBooks({ bookIds });
  };

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle={`선택한 ${bookIds.length}권의 책을 서재에서 삭제할까?`}>
          책 삭제
        </Dialog.Header>

        <Dialog.Footer>
          <ButtonStack>
            <Button type="button" variant="ghost" size="large" block onClick={onClose}>
              취소
            </Button>
            <Button type="button" variant="primary" size="large" block onClick={handleDeleteBooks}>
              삭제
            </Button>
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
