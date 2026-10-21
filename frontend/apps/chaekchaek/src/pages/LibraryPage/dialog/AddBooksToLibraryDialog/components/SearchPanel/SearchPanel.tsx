import { useCallback, useEffect, useState } from 'react';
import type { ChangeEvent } from 'react';

import { ImgBox, Input, List, ScrollArea } from '@chaekchaek/design-system';

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

  return (
    <>
      <Input sx={{ mt: 2, mb: 4 }} size="small" value={query} onChange={handleChange} />

      <ScrollArea height="320px">
        <List>
          {booksData?.map((item) => {
            return (
              <List.Item key={item.isbn13}>
                <List.Item.Leading>
                  <ImgBox img={item.coverImageUrl} size="x-small" />
                </List.Item.Leading>
                <List.Item.Content
                  ellipsis
                  title={<>{item.title}</>}
                  content={item.authors.join(' · ')}
                />
                <List.Item.Trailing>담긴 책</List.Item.Trailing>
              </List.Item>
            );
          })}
        </List>
      </ScrollArea>
    </>
  );
};
