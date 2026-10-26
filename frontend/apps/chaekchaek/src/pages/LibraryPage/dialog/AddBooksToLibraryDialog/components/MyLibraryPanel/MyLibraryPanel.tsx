import { useCallback, useState, type ChangeEvent } from 'react';
import { Link } from 'react-router-dom';

import { Button, Checkbox, Icon, ImgBox, Input, List, ScrollArea } from '@chaekchaek/design-system';

import { getLibraries } from '@/services/apis/libraries/repository';

import { useLoadData } from '@/services/core/useLoadData';

import type { MyLibraryPanelProps } from './MyLibraryPanel.types';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

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

  return (
    <>
      <Input
        sx={{ mt: 2, mb: 4 }}
        size="small"
        leading={<Icon.SearchIcon />}
        placeholder="책 제목이나 작가를 검색하세요"
        value={query}
        onChange={handleChange}
      />

      <ScrollArea height="340px">
        <List>
          {queryFilteredBooks?.map((item) => {
            const isIncluded = selection.includes(item.isbn13);

            const isRegisteredInCurrentLibrary = !!currentLibraryBooks.find(
              (currentLibraryBook) => currentLibraryBook.isbn13 === item.isbn13,
            );

            return (
              <List.Item key={item.bookId}>
                <List.Item.Leading>
                  <Checkbox
                    disabled={isRegisteredInCurrentLibrary}
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
      <Button block variant="primary" onClick={handleSubmit}>
        담기
      </Button>
    </>
  );
};
