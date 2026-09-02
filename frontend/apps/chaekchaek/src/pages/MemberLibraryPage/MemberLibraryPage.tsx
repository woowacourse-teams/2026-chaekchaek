import { useCallback, useEffect } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import {
  Icon,
  ImgBox,
  List,
  Notice,
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

import { getMembersMemberIdLibrary } from '@/services/apis/membersMemberIdLibrary/repository';
import { useLoadData } from '@/services/core/useLoadData';

import { ROUTES } from '@/constants/routes';

export const READING_STATUS = {
  ALL: 'ALL',
  WANT_TO_READ: 'WANT_TO_READ',
  READING: 'READING',
  FINISHED: 'FINISHED',
} as const;

export type ReadingStatus = (typeof READING_STATUS)[keyof typeof READING_STATUS];

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

export const MemberLibraryPage = () => {
  const { memberId: memberIdString } = useParams<{ memberId: string }>();
  const memberId = Number(memberIdString);

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

  const getMembersMemberIdLibraryLoadData = useCallback(async () => {
    return await getMembersMemberIdLibrary({
      memberId,
      page: defaultPage,
      sort,
      status,
    });
  }, [defaultPage, status, sort]);
  const {
    status: { data: libraryData, error },
  } = useLoadData({ queryFn: getMembersMemberIdLibraryLoadData });

  const filteredTotalPages = libraryData ? Math.ceil(libraryData.filteredCount / 10) : 1;

  const navigation = useNavigate();
  useEffect(() => {
    if (error)
      navigation(ROUTES.HOME, {
        replace: true,
      });
  }, [error]);

  if (error) return null;

  return (
    <Layout>
      <Header />
      <Main>
        <Split>
          <Split.Top sx={{ mb: 6 }}>
            <Title level="page">내 서재</Title>
          </Split.Top>
          <Split.Side>
            <Title
              level="main"
              orientation="vertical"
              trailing={
                <OptionList
                  sx={{ mt: 3 }}
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
            {!libraryData?.items.length && (
              <Notice height={500}>내 서재에 등록된 책이 없습니다.</Notice>
            )}
            <List columns={2}>
              {libraryData?.items.map((item) => {
                return (
                  <List.Item key={item.bookId}>
                    <List.Item.Leading>
                      <Link to={`/books/${item.isbn13}`}>
                        <ImgBox img={item.coverImageUrl} size="small" />
                      </Link>
                    </List.Item.Leading>
                    <List.Item.Content
                      as={Link}
                      to={`/books/${item.isbn13}`}
                      title={
                        <>
                          <Tag
                            sx={{ mb: 2 }}
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
                      <Icon.ArrowRightIcon />
                    </List.Item.Trailing>
                  </List.Item>
                );
              })}
            </List>

            <Pagination
              sx={{ mt: 5 }}
              defaultPage={defaultPage}
              totalPages={filteredTotalPages}
              onChange={handleChangeDefaultPage}
            />
          </Split.Content>
        </Split>
        s
      </Main>
    </Layout>
  );
};
