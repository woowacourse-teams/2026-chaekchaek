import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ChangeEvent } from 'react';

import { Button, Checkbox, ConstrainedImgBox, Dialog, List } from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { getLibraries } from '@/services/apis/libraries/repository';
import { useExecute } from '@/services/core/useExecute';
import { postLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import type { RegisterBookToLibraryDialogProps } from './RegisterBookToLibraryDialog.types';
import { deleteLibrariesLibraryIdBooksBookId } from '@/services/apis/librariesLibraryIdBooksBookId/repository';

type LibraryId = number;

export const RegisterBookToLibraryDialog = ({
  book,
  onClose,
}: RegisterBookToLibraryDialogProps) => {
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

  const initialSelection = useMemo(() => {
    return librariesData?.libraries
      .filter((library) => {
        return library.items.find((item) => book.isbn === item.isbn13);
      })
      .map((library) => library.libraryId);
  }, [librariesData, book.bookId]);

  useEffect(() => {
    if (!initialSelection) return;

    setSelection(initialSelection);
  }, [initialSelection]);

  const [selection, setSelection] = useState<LibraryId[]>([]);
  const handleChangeSelection = (e: ChangeEvent<HTMLInputElement>) => {
    const libraryId = Number(e.target.value);

    setSelection((prev) => {
      const isIncluded = selection.includes(libraryId);
      return isIncluded ? prev.filter((item) => item !== libraryId) : [...prev, libraryId];
    });
  };

  const addBookToLibrary = async (addedLibraries: number[]) => {
    return await Promise.all(
      addedLibraries.map((libraryId) =>
        postLibrariesLibraryId({
          libraryId,
          isbn13: book.isbn,
          status: 'READING',
        }),
      ),
    );
  };

  const deleteBookToLibrary = async (deletedLibraries: number[]) => {
    const bookId = book.bookId;
    if (!bookId) return;

    return await Promise.all(
      deletedLibraries.map((libraryId) =>
        deleteLibrariesLibraryIdBooksBookId({
          libraryId,
          bookId,
        }),
      ),
    );
  };

  const { mutate: postLibrariesLibraryIdMutate } = useExecute({
    executeFn: addBookToLibrary,
  });

  const { mutate: deleteLibrariesLibraryIdBooksBookIdMutate } = useExecute({
    executeFn: deleteBookToLibrary,
  });

  const handleSubmit = async () => {
    const initialLibraryIds = initialSelection ?? [];
    const addedLibraries = selection?.filter((libraryId) => {
      return !initialLibraryIds.includes(libraryId);
    });

    const deletedLibraries = initialLibraryIds.filter((libraryId) => {
      return !selection.includes(libraryId);
    });

    const requests: Promise<unknown>[] = [];

    if (addedLibraries.length > 0) {
      requests.push(postLibrariesLibraryIdMutate(addedLibraries));
    }

    if (deletedLibraries.length > 0) {
      requests.push(deleteLibrariesLibraryIdBooksBookIdMutate(deletedLibraries));
    }

    await Promise.all(requests);

    onClose();
  };

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle={`책을 보관할 책장을 선택해 주세요.`}>책장 선택하기</Dialog.Header>
        <Dialog.Body>
          <List>
            {librariesData?.libraries.map((library) => {
              const isIncluded = selection.includes(library.libraryId);

              return (
                <List.Item key={library.libraryId}>
                  <List.Item.Leading>
                    {library.items.map((item) => {
                      return <ConstrainedImgBox img={item.spineImageUrl} height="40px" />;
                    })}
                  </List.Item.Leading>
                  <List.Item.Content title={library.name} content={`${library.items.length}권`} />
                  <List.Item.Trailing>
                    <Checkbox
                      size="large"
                      checked={isIncluded}
                      value={library.libraryId}
                      onChange={handleChangeSelection}
                    />
                  </List.Item.Trailing>
                </List.Item>
              );
            })}
          </List>
        </Dialog.Body>
        <Dialog.Footer>
          <Button block variant="primary" onClick={handleSubmit}>
            완료
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
