import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  Button,
  ImgBox,
  List,
  OptionList,
  Pagination,
  ProgressBar,
  Select,
  Split,
  Tag,
  Title,
} from '@chaekchaek/design-system';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { getLibrary } from '@/services/apis/library/repository';
import { useLoadData } from '@/services/core/useLoadData';

export const LibraryPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const defaultPage = searchParams.get('page') ? Number(searchParams.get('page')) : 1;
  const status = searchParams.get('status') ?? '';

  const handleChangeDefaultPage = (defaultPage: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', String(defaultPage));
      return next;
    });
  };

  const handleChangeStatus = (status: '' | 'WANT_TO_READ' | 'READING' | 'FINISHED') => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('status', status);
      next.set('page', '1');
      return next;
    });
  };

  const getLibraryLoadData = useCallback(async () => {
    return await getLibrary({
      page: defaultPage,
      sort: '',
      status,
    });
  }, [defaultPage, status]);
  const {
    status: { data: libraryData },
  } = useLoadData({ queryFn: getLibraryLoadData });

  return (
    <Layout>
      <Header />
      <Main>
        <Title
          level="page"
          trailing={
            <>
              <Button variant="ghost">감상 익명 공개</Button>
              <Button variant="primary">서재 편집</Button>
            </>
          }
        >
          내 서재
        </Title>
        <Split>
          <Split.Side>
            <Title
              level="main"
              orientation="vertical"
              trailing={
                <OptionList
                  value={status}
                  options={[
                    {
                      value: '',
                      text: '전체',
                      meta: '0',
                    },
                    {
                      value: 'WANT_TO_READ',
                      text: '읽고 싶어요',
                      meta: '0',
                    },
                    {
                      value: 'READING',
                      text: '읽는 중',
                      meta: '0',
                    },
                    {
                      value: 'FINISHED',
                      text: '다 읽음',
                      meta: '0',
                    },
                  ]}
                  onChange={handleChangeStatus}
                />
              }
            >
              독서 상태
            </Title>
          </Split.Side>
          <Split.Content>
            <Title
              level="caption"
              trailing={
                <>
                  <Select
                    value="RECENT"
                    options={[
                      { value: 'RECENT', text: '최근순' },
                      { value: 'OLDEST', text: '오래된순' },
                      { value: 'COMMENT', text: '감상순' },
                      { value: 'RATING', text: '별점순' },
                      { value: 'TITLE', text: '제목순' },
                    ]}
                  />
                </>
              }
            >
              독서 상태
            </Title>
            <List>
              {libraryData?.items.map((item) => {
                return (
                  <List.Item key={item.bookId}>
                    <List.Item.Leading>
                      <a href={`/books/${item.isbn13}`}>
                        <ImgBox img={item.coverImageUrl} size="small" />
                      </a>
                    </List.Item.Leading>
                    <List.Item.Content
                      title={
                        <>
                          <Tag variant={item.status === 'READING' ? 'primary' : 'subtle'}>
                            {item.status === 'WANT_TO_READ'
                              ? '읽고 싶어요'
                              : item.status === 'READING'
                                ? '읽는 중'
                                : item.status === 'FINISHED'
                                  ? '다 읽음'
                                  : ''}
                          </Tag>
                          <br />
                          {item.title}
                        </>
                      }
                      content={item.authors.join(' · ')}
                      description={
                        <>
                          {item.totalPages > 0 && (
                            <ProgressBar value={item.currentPage} max={item.totalPages} />
                          )}
                        </>
                      }
                    />
                    <List.Item.Trailing>&gt;</List.Item.Trailing>
                  </List.Item>
                );
              })}
            </List>

            <Pagination
              defaultPage={1}
              totalPages={libraryData?.filteredCount ?? 1}
              onChange={handleChangeDefaultPage}
            />
          </Split.Content>
        </Split>
      </Main>
    </Layout>
  );
};
