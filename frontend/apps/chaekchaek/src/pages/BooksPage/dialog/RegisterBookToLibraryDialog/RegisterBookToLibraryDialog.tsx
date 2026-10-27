import { useCallback } from 'react';

import { Button, Checkbox, ConstrainedImgBox, Dialog, List } from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { getLibraries } from '@/services/apis/libraries/repository';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import type { RegisterBookToLibraryDialogProps } from './RegisterBookToLibraryDialog.types';

export const RegisterBookToLibraryDialog = ({ onClose }: RegisterBookToLibraryDialogProps) => {
  const { user } = useAuthContext();

  const getLibrariesLoadData = useCallback(async () => {
    if (!user?.memberId) return;
    return await getLibraries({ memberId: user?.memberId });
  }, [user?.memberId]);

  const {
    status: { data: librariesData },
  } = useLoadData({
    queryFn: getLibrariesLoadData,
  });

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle={`저장할 책장 하나 이상을 선택해 주세요.`}>
          저장할 책장 고르기
        </Dialog.Header>
        <Dialog.Body>
          <List>
            {librariesData?.libraries.map((library) => {
              return (
                <List.Item key={library.libraryId}>
                  <List.Item.Leading>
                    {library.items.map((item) => {
                      return <ConstrainedImgBox img={item.spineImageUrl} height="40px" />;
                    })}
                  </List.Item.Leading>
                  <List.Item.Content title={library.name} content={`${library.items.length}권`} />
                  <List.Item.Trailing>
                    <Checkbox size="large" />
                  </List.Item.Trailing>
                </List.Item>
              );
            })}
          </List>
        </Dialog.Body>
        <Dialog.Footer>
          <Button block variant="primary">
            담기
          </Button>
        </Dialog.Footer>
        ;
      </Dialog.Container>
    </Dialog>
  );
};
