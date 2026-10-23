import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';

import { Checkbox, Icon, ImgBox, Input, List, ScrollArea } from '@chaekchaek/design-system';

import { getBooks } from '@/services/apis/books/repository';
import { useInfiniteLoadData } from '@/services/core/useInfiniteLoadData';

export const SearchPanel = () => {
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

  const [selection, setSelection] = useState<string[]>([]);
  const handleChangeSelection = (e: ChangeEvent<HTMLInputElement>) => {
    const isbn = e.target.value;

    setSelection((prev) => {
      const isIncluded = selection.includes(isbn);
      return isIncluded ? prev.filter((item) => item === isbn) : [...prev, isbn];
    });
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

      <ScrollArea ref={scrollAreaRef} height="320px">
        <List>
          {booksData?.map((item) => {
            const isIncluded = selection.includes(item.isbn13);

            return (
              <List.Item key={item.isbn13}>
                <List.Item.Leading>
                  <Checkbox
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
                {/* <List.Item.Trailing>담긴 책</List.Item.Trailing> */}
              </List.Item>
            );
          })}
        </List>
      </ScrollArea>
    </>
  );
};
