import { useState } from 'react';

import { Dialog, Tabs } from '@chaekchaek/design-system';

import { postLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';
import { useExecute } from '@/services/core/useExecute';

import { MyLibraryPanel } from './components/MyLibraryPanel';
import { SearchPanel } from './components/SearchPanel';

import type { AddBooksToLibraryDialogProps } from './AddBooksToLibraryDialog.types';

type TabType = 'my-library' | 'search';

export const AddBooksToLibraryDialog = ({
  libraryId,
  onBookAdded,
  onClose,
}: AddBooksToLibraryDialogProps) => {
  const [active, setActive] = useState<TabType>('my-library');
  const handleChange = (active: string | number) => {
    setActive(active as TabType);
  };

  const { mutate: postLibrariesLibraryIdMutate } = useExecute({
    executeFn: postLibrariesLibraryId,
  });

  const addBookToLibrary = async (isbns: string[]) => {
    await Promise.all(
      isbns.map((isbn) =>
        postLibrariesLibraryIdMutate({
          libraryId,
          isbn13: isbn,
          status: 'READING',
        }),
      ),
    );

    await onBookAdded();

    onClose();
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
                panel: <MyLibraryPanel libraryId={libraryId} onSubmit={addBookToLibrary} />,
              },
              {
                text: '책 검색',
                value: 'search',
                panel: <SearchPanel onSubmit={addBookToLibrary} />,
              },
            ]}
            onChange={handleChange}
          />
        </Dialog.Body>
      </Dialog.Container>
    </Dialog>
  );
};
