import { useState } from 'react';

import { Button, Dialog, Tabs } from '@chaekchaek/design-system';

import { MyLibraryPanel } from './components/MyLibraryPanel';
import { SearchPanel } from './components/SearchPanel';

import type { AddBooksToLibraryDialogProps } from './AddBooksToLibraryDialog.types';

type TabType = 'my-library' | 'search';

export const AddBooksToLibraryDialog = ({ onClose }: AddBooksToLibraryDialogProps) => {
  const [active, setActive] = useState<TabType>('my-library');
  const handleChange = (active: string | number) => {
    setActive(active as TabType);
  };

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>책장에 책 담기</Dialog.Header>
        <Dialog.Body>
          <Tabs
            style={{ minHeight: '500px' }}
            distribution="equal"
            active={active}
            items={[
              {
                text: '내 서재에서',
                value: 'my-library',
                panel: <MyLibraryPanel />,
              },
              {
                text: '책 검색',
                value: 'search',
                panel: <SearchPanel />,
              },
            ]}
            onChange={handleChange}
          />
        </Dialog.Body>
      </Dialog.Container>
    </Dialog>
  );
};
