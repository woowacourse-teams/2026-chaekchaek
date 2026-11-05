import { useCallback, useState, type ChangeEvent } from 'react';

import {
  Button,
  Checkbox,
  Icon,
  ImgBox,
  InfoMessage,
  Input,
  List,
  ScrollArea,
} from '@chaekchaek/design-system';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { getLibraries } from '@/services/apis/libraries/repository';
import { useLoadData } from '@/services/core/useLoadData';

import { getInfoMessage, getLibraryStatus } from '../../AddBooksToLibraryDialog';
import type { MyLibraryPanelProps } from './MyLibraryPanel.types';

export const MyLibraryPanel = ({ libraryId, onSubmit }: MyLibraryPanelProps) => {
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

  const filteredLibraries = librariesData?.libraries.filter(
    (library) => library.libraryId !== libraryId,
  );

  const currentLibrary = librariesData?.libraries.find(
    (library) => library.libraryId === libraryId,
  );
  const currentLibraryBooks = currentLibrary ? currentLibrary.items : [];

  const filteredBooks = [
    ...new Map(
      filteredLibraries?.flatMap((library) => library.items).map((item) => [item.isbn13, item]) ??
        [],
    ).values(),
  ];

  const [query, setQuery] = useState('');
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
  };

  const queryFilteredBooks = filteredBooks.filter(
    (book) => book.title.includes(query) || book.authors.some((author) => author.includes(query)),
  );

  const [selection, setSelection] = useState<string[]>([]);
  const handleChangeSelection = (e: ChangeEvent<HTMLInputElement>) => {
    const isbn = e.target.value;

    setSelection((prev) => {
      const isIncluded = selection.includes(isbn);
      return isIncluded ? prev.filter((item) => item === isbn) : [...prev, isbn];
    });
  };

  const handleSubmit = async () => {
    onSubmit(selection);
  };

  const maxSize = currentLibrary?.maxSize;
  const totalBookCount = (currentLibrary?.items.length ?? 0) + selection.length;
  const libraryStatus = getLibraryStatus(totalBookCount, maxSize);
  const isLimitReached = libraryStatus.status === 'full';
  const infoMessage = getInfoMessage(libraryStatus);

  return (
    <>
      <Input
        sx={{ mt: 2, mb: 1 }}
        size="small"
        leading={<Icon.SearchIcon />}
        placeholder="책 제목이나 작가를 검색하세요"
        value={query}
        onChange={handleChange}
      />

      <InfoMessage title={infoMessage} />

      <ScrollArea height="310px">
        <List>
          {queryFilteredBooks?.map((item) => {
            const isIncluded = selection.includes(item.isbn13);

            const isRegisteredInCurrentLibrary = !!currentLibraryBooks.find(
              (currentLibraryBook) => currentLibraryBook.isbn13 === item.isbn13,
            ); // 현재 책장에 담겨 있는 책

            const isSelectionDisabled = !isIncluded && isLimitReached; // 최대 권수를 채운 경우 더 추가하지 못하게

            const isCheckDisabled = isRegisteredInCurrentLibrary || isSelectionDisabled;

            return (
              <List.Item key={item.bookId}>
                <List.Item.Leading>
                  <Checkbox
                    sx={{ mr: 2 }}
                    size="large"
                    disabled={isCheckDisabled}
                    value={item.isbn13}
                    checked={isIncluded}
                    onChange={handleChangeSelection}
                  />
                  <ImgBox img={item.coverImageUrl} size="x-small" />
                </List.Item.Leading>
                <List.Item.Content
                  ellipsis
                  title={<>{item.title}</>}
                  content={item.authors.join(' · ')}
                />
                {isRegisteredInCurrentLibrary && <List.Item.Trailing>담긴 책</List.Item.Trailing>}
              </List.Item>
            );
          })}
        </List>
      </ScrollArea>
      <Button block variant="primary" disabled={!selection.length} onClick={handleSubmit}>
        담기
      </Button>
    </>
  );
};
