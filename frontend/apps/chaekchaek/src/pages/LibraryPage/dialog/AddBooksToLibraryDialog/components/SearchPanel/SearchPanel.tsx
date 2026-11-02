import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';

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

import { getBooks } from '@/services/apis/books/repository';
import { useInfiniteLoadData } from '@/services/core/useInfiniteLoadData';

import { getLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';
import { useLoadData } from '@/services/core/useLoadData';

import { LIBRARY_BOOK_MAX_LENGTH } from '../../AddBooksToLibraryDialog';
import type { SearchPanelProps } from './SearchPanel.types';

export const SearchPanel = ({ libraryId, onSubmit }: SearchPanelProps) => {
  const [query, setQuery] = useState('');
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
  };

  const getBooksLoadData = useCallback(
    async ({ pageParam }: { pageParam: number }) => {
      return await getBooks({ page: pageParam, query });
    },
    [query],
  );

  const {
    status: { data: booksDataAll },
    fetchNextPage,
  } = useInfiniteLoadData({
    queryFn: getBooksLoadData,
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      return lastPage?.nextPage || 1;
    },
  });

  const booksData = (booksDataAll?.pages ?? []).flatMap((page) => page?.items).filter(Boolean);

  const scrollAreaRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!scrollAreaRef.current) return;
    const handleScroll = async () => {
      if (!scrollAreaRef.current) return;
      const { scrollTop, clientHeight, scrollHeight } = scrollAreaRef.current;

      const isBottom = scrollTop + clientHeight >= scrollHeight;

      if (isBottom) {
        if (booksDataAll?.pages?.[booksDataAll?.pages.length - 1]?.nextPage === null) {
          return;
        }
        await fetchNextPage();
      }
    };
    scrollAreaRef.current.addEventListener('scroll', handleScroll);

    return () => {
      scrollAreaRef.current?.removeEventListener('scroll', handleScroll);
    };
  }, [booksDataAll]);

  useEffect(() => {
    scrollAreaRef.current?.scrollTo({ top: 0 });
  }, [query]);

  const getLibrariesLibraryIdLoadData = useCallback(async () => {
    return await getLibrariesLibraryId({
      libraryId,
    });
  }, [libraryId]);

  const {
    status: { data: currentLibraryBooks },
  } = useLoadData({
    queryFn: getLibrariesLibraryIdLoadData,
  });

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

  const totalBookCount = (currentLibraryBooks?.items.length ?? 0) + selection.length;
  const remainingBookCount = LIBRARY_BOOK_MAX_LENGTH - totalBookCount;
  const isLimitReached = remainingBookCount <= 0;

  const infoMessage = isLimitReached
    ? '책장이 가득 찼어요. 책을 추가하려면 선택을 해제해 주세요.'
    : `${remainingBookCount}권 더 담을 수 있어요 (${totalBookCount}/${LIBRARY_BOOK_MAX_LENGTH})`;

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

      <ScrollArea ref={scrollAreaRef} height="310px">
        <List>
          {booksData?.map((item) => {
            const isIncluded = selection.includes(item.isbn13);

            const isRegisteredInCurrentLibrary = !!currentLibraryBooks?.items.find(
              (currentLibraryBook) => currentLibraryBook.isbn13 === item.isbn13,
            );

            return (
              <List.Item key={item.isbn13}>
                <List.Item.Leading>
                  <Checkbox
                    value={item.isbn13}
                    checked={isIncluded}
                    disabled={isRegisteredInCurrentLibrary}
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
