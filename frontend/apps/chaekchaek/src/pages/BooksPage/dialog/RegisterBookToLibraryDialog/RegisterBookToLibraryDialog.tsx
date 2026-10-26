import { Button, Checkbox, ConstrainedImgBox, Dialog, List } from '@chaekchaek/design-system';

import type { RegisterBookToLibraryDialogProps } from './RegisterBookToLibraryDialog.types';

import DummyImg1 from '../../../../../../../packages/design-system/src/components/ConstrainedImgBox/imgs/dummy1.jpg';

export const RegisterBookToLibraryDialog = ({ onClose }: RegisterBookToLibraryDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle={`저장할 책장 하나 이상을 선택해 주세요.`}>
          저장할 책장 고르기
        </Dialog.Header>
        <Dialog.Body>
          <List>
            {Array.from({ length: 4 }).map((_, index) => {
              const library = {
                name: 'name' + index,
                items: Array.from({ length: 10 }).map(() => ({
                  spineImageUrl: DummyImg1,
                })),
              };
              return (
                <List.Item>
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
