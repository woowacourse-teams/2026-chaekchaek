import { useState } from 'react';

import { Dialog, Tabs } from '@chaekchaek/design-system';

import { postLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';
import { useExecute } from '@/services/core/useExecute';

import { track } from '@/analytics/track';

import { MyLibraryPanel } from './components/MyLibraryPanel';
import { SearchPanel } from './components/SearchPanel';

import type { AddBooksToLibraryDialogProps } from './AddBooksToLibraryDialog.types';

type TabType = 'my-library' | 'search';

export const getLibraryStatus = (totalBookCount: number, maxSize: number | undefined) => {
  if (maxSize === undefined) return { status: 'loading' } as const;

  const remainingBookCount = maxSize - totalBookCount;

  return {
    status: remainingBookCount ? 'available' : 'full',
    maxSize,
    totalBookCount,
    remainingBookCount,
  } as const;
};

export const getInfoMessage = (libraryStatus: ReturnType<typeof getLibraryStatus>) => {
  switch (libraryStatus.status) {
    case 'loading':
      return '';

    case 'full':
      return '책장이 가득 찼어요. 책을 추가하려면 선택을 해제해 주세요.';
    case 'available':
      return `${libraryStatus.remainingBookCount}권 더 담을 수 있어요 (${libraryStatus.totalBookCount}/${libraryStatus.maxSize})`;
    default:
      return '';
  }
};

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

    track('library_add', {
      source: 'library',
      status: 'reading',
    });

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
                panel: <SearchPanel libraryId={libraryId} onSubmit={addBookToLibrary} />,
              },
            ]}
            onChange={handleChange}
          />
        </Dialog.Body>
      </Dialog.Container>
    </Dialog>
  );
};
