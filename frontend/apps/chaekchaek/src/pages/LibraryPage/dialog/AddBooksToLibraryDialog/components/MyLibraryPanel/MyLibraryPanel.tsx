import { useCallback } from 'react';
import { Link } from 'react-router-dom';

import { Button, Icon, ImgBox, Input, List, ScrollArea } from '@chaekchaek/design-system';

import { getLibraries } from '@/services/apis/libraries/repository';

import { useLoadData } from '@/services/core/useLoadData';

import type { MyLibraryPanelProps } from './MyLibraryPanel.types';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

export const MyLibraryPanel = ({ libraryId }: MyLibraryPanelProps) => {
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

  const filteredBooks = [
    ...new Map(
      filteredLibraries?.flatMap((library) => library.items).map((item) => [item.isbn13, item]) ??
        [],
    ).values(),
  ];

  return (
    <>
      <Input
        sx={{ mt: 2, mb: 4 }}
        size="small"
        leading={<Icon.SearchIcon />}
        placeholder="책 제목이나 작가를 검색하세요"
      />

      <ScrollArea height="340px">
        <List>
          {filteredBooks?.map((item) => {
            return (
              <List.Item key={item.bookId}>
                <List.Item.Leading>
                  <Link to={`/books/${item.isbn13}`}>
                    {/* <ImgBox img={item.coverImageUrl} size="x-small" /> */}
                  </Link>
                </List.Item.Leading>
                <List.Item.Content
                  ellipsis
                  title={<>{item.title}</>}
                  // content={item.authors.join(' · ')}
                />
              </List.Item>
            );
          })}
        </List>
      </ScrollArea>
      <Button block variant="primary" onClick={() => {}}>
        담기
      </Button>
    </>
  );
};
