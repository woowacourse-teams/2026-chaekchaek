import { Button, ButtonStack, Dialog } from '@chaekchaek/design-system';

import type { DeleteLibraryDialogProps } from './DeleteLibraryDialog.types';

import { deleteLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';
import { useExecute } from '@/services/core/useExecute';

export const DeleteLibraryDialog = ({
  libraryId,
  libraryName,
  onClose,
}: DeleteLibraryDialogProps) => {
  const { mutate: deleteLibrariesLibraryIdMutate } = useExecute({
    executeFn: deleteLibrariesLibraryId,
  });

  const handleClickDeleteLibrary = async () => {
    await deleteLibrariesLibraryIdMutate({ libraryId });
    onClose();
  };
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle={`‘${libraryName}’ 책장을 삭제합니다.`}>
          책장을 삭제할까요?
        </Dialog.Header>
        <Dialog.Footer>
          <ButtonStack>
            <Button block variant="ghost">
              취소
            </Button>
            <Button block variant="primary" onClick={handleClickDeleteLibrary}>
              책장 삭제
            </Button>
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
