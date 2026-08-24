import { useCallback, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  Button,
  Checkbox,
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

export const READING_STATUS = {
  ALL: 'ALL',
  WANT_TO_READ: 'WANT_TO_READ',
  READING: 'READING',
  FINISHED: 'FINISHED',
} as const;

type ReadingStatus = (typeof READING_STATUS)[keyof typeof READING_STATUS];

export const READING_STATUS_LABELS: Record<ReadingStatus, string> = {
  [READING_STATUS.ALL]: '전체',
  [READING_STATUS.WANT_TO_READ]: '읽고 싶어요',
  [READING_STATUS.READING]: '읽는 중',
  [READING_STATUS.FINISHED]: '다 읽음',
};

const READING_SORT = {
  RECENT: 'RECENT',
  OLDEST: 'OLDEST',
  COMMENT: 'COMMENT',
  RATING: 'RATING',
  TITLE: 'TITLE',
} as const;

type ReadingSort = (typeof READING_SORT)[keyof typeof READING_SORT];

const READING_SORT_LABELS = {
  RECENT: '최근순',
  OLDEST: '오래된순',
  COMMENT: '감상순',
  RATING: '별점순',
  TITLE: '제목순',
} as const;

export const LibraryPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const defaultPage = searchParams.get('page') ? Number(searchParams.get('page')) : 1;
  const status = (searchParams.get('status') ?? READING_STATUS.ALL) as ReadingStatus;
  const sort = (searchParams.get('sort') ?? READING_SORT.RECENT) as ReadingSort;

  const handleChangeDefaultPage = (defaultPage: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', String(defaultPage));
      return next;
    });
  };

  const handleChangeStatus = (status: ReadingStatus) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('status', status);
      next.set('page', '1');
      return next;
    });
  };

  const handleChangeSort = (sort: ReadingSort) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('sort', sort);
      next.set('page', '1');
      return next;
    });
  };

  const getLibraryLoadData = useCallback(async () => {
    return await getLibrary({
      page: defaultPage,
      sort,
      status,
    });
  }, [defaultPage, status, sort]);
  const {
    status: { data: libraryData },
  } = useLoadData({ queryFn: getLibraryLoadData });

  const [editing, setEditing] = useState(false);
  const handleClickEditingStart = () => {
    setEditing(true);
  };
  const handleClickEditingEnd = () => {
    setEditing(false);
  };

  return (
    <Layout>
      <Header />
      <Main>
        <Title
          level="page"
          trailing={
            <>
              <Button variant="ghost">감상 익명 공개</Button>
              {editing && (
                <>
                  <Button variant="ghost">상태 변경</Button>
                  <Button variant="ghost">삭제</Button>
                </>
              )}
              <Button variant="primary" onClick={handleClickEditingStart}>
                서재 편집
              </Button>
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
                  options={Object.entries(READING_STATUS_LABELS).map(([labelKey, labelValue]) => {
                    return {
                      value: labelKey,
                      text: labelValue,
                    };
                  })}
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
                    value={sort}
                    options={Object.entries(READING_SORT_LABELS).map(([labelKey, labelValue]) => {
                      return {
                        value: labelKey,
                        text: labelValue,
                      };
                    })}
                    onChange={handleChangeSort}
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
                      {editing && <Checkbox />}
                      <a href={`/books/${item.isbn13}`}>
                        <ImgBox img={item.coverImageUrl} size="small" />
                      </a>
                    </List.Item.Leading>
                    <List.Item.Content
                      title={
                        <>
                          <Tag
                            variant={item.status === READING_STATUS.READING ? 'primary' : 'subtle'}
                          >
                            {READING_STATUS_LABELS?.[item.status as ReadingStatus] ?? ''}
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
                    <List.Item.Trailing>
                      {!editing && <>&gt;</>}
                      {editing && <Button>삭제</Button>}
                    </List.Item.Trailing>
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
