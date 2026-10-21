import { Button, Dialog, Tabs } from '@chaekchaek/design-system';

import { MyLibraryPanel } from './components/MyLibraryPanel';

import type { AddBooksToLibraryDialogProps } from './AddBooksToLibraryDialog.types';

export const AddBooksToLibraryDialog = ({ onClose }: AddBooksToLibraryDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>책장에 책 담기</Dialog.Header>
        <Dialog.Body>
          <Tabs
            style={{ minHeight: '400px' }}
            distribution="equal"
            active="a"
            items={[
              {
                text: '내 서재에서',
                value: 'a',
                panel: <MyLibraryPanel />,
              },
              {
                text: '책 검색',
                value: 'b',
                panel: <></>,
              },
            ]}
            onChange={() => {}}
          />
        </Dialog.Body>
        <Dialog.Footer>
          <Button block variant="primary">
            담기
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
