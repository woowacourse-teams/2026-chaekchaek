import { useCallback, useEffect, useState } from 'react';
import type { ChangeEvent } from 'react';

import { Button, Checkbox, ConstrainedImgBox, Dialog, List } from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { getLibraries } from '@/services/apis/libraries/repository';
import { useExecute } from '@/services/core/useExecute';
import { postLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import type { RegisterBookToLibraryDialogProps } from './RegisterBookToLibraryDialog.types';

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

  useEffect(() => {
    if (!librariesData) return;
    const selection = librariesData.libraries
      .filter((library) => {
        return library.items.find((item) => book.isbn === item.isbn13);
      })
      .map((library) => library.libraryId);

    setSelection(selection);
  }, [librariesData]);

  const [selection, setSelection] = useState<LibraryId[]>([]);
  const handleChangeSelection = (e: ChangeEvent<HTMLInputElement>) => {
    const libraryId = Number(e.target.value);

    setSelection((prev) => {
      const isIncluded = selection.includes(libraryId);
      return isIncluded ? prev.filter((item) => item === libraryId) : [...prev, libraryId];
    });
  };

  const addBookToLibrary = async () => {
    return await Promise.all(
      selection.map((libraryId) =>
        postLibrariesLibraryId({
          libraryId,
          isbn13: book.isbn,
          status: 'READING',
        }),
      ),
    );
  };

  const { mutate: postLibrariesLibraryIdMutate } = useExecute({
    executeFn: addBookToLibrary,
  });

  const handleSubmit = async () => {
    await postLibrariesLibraryIdMutate();

    onClose();
  };

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header subTitle={`저장할 책장 하나 이상을 선택해 주세요.`}>
          저장할 책장 고르기
        </Dialog.Header>
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
            담기
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
