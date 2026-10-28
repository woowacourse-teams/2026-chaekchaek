import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import {
  Avatar,
  Icon,
  IconButton,
  ImgBox,
  List,
  Notice,
  Pagination,
  Select,
  Split,
  Title,
} from '@chaekchaek/design-system';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

export const MemberLibraryPage = () => {
  const { memberId: memberIdString } = useParams<{ memberId: string }>();
  const memberId = Number(memberIdString);

  const [searchParams, setSearchParams] = useSearchParams();
  const defaultPage = searchParams.get('page') ? Number(searchParams.get('page')) : 1;
  const sort = (searchParams.get('sort') ?? READING_SORT.RECENT) as ReadingSort;

  const handleChangeDefaultPage = (defaultPage: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', String(defaultPage));
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
      status: READING_STATUS.ALL,
    });
  }, [defaultPage, sort]);
  const {
    status: { data: libraryData, error },
  } = useLoadData({ queryFn: getMembersMemberIdLibraryLoadData });

  const filteredTotalPages = libraryData ? Math.ceil(libraryData.filteredCount / 10) : 1;

  const [isCopied, setIsCopied] = useState(false);

  useEffect(() => {
    const TIME_OUT = 1500;
    if (isCopied)
      setTimeout(() => {
        setIsCopied(false);
      }, TIME_OUT);
  }, [isCopied]);

  const handleCopyLink = async () => {
    const memberLibraryLink = window.location.href;
    await navigator.clipboard.writeText(memberLibraryLink);

    setIsCopied(true);
  };

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
        <Container>
          <Split>
            <Split.Top sx={{ mb: 14 }}>
              <Title level="page">
                {libraryData?.member.profileImageUrl && (
                  <Avatar sx={{ mt: 2, mr: 2 }} img={libraryData?.member.profileImageUrl} />
                )}
                '{libraryData?.member.displayName}' 서재
                <IconButton shape="link" size="large" onClick={handleCopyLink}>
                  {!isCopied ? (
                    <Icon.ShareIcon size="x-large" color="secondary" />
                  ) : (
                    <Icon.CheckAnimatedIcon size="x-large" color="secondary" />
                  )}
                </IconButton>
              </Title>
            </Split.Top>
            <Split.Side>
              <Title style={{ height: '800px' }} level="main" orientation="vertical">
                독서 상태
              </Title>
            </Split.Side>
            <Split.Content>
              <Title
                level="caption"
                sx={{ mb: 10 }}
                trailing={
                  <>
                    <Select
                      size="small"
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

              <div className={styles.bookShelf}>
                {libraryData?.items.map((item, index) => {
                  return (
                    <Link to={`/books/${item.isbn13}`}>
                      <Book3D
                        key={index}
                        coverImageUrl={item.coverImageUrl}
                        spineImageUrl={item.spineImageUrl ?? item.coverImageUrl}
                        backImageUrl={item.backImageUrl ?? item.coverImageUrl}
                      />
                    </Link>
                  );
                })}
              </div>

              {!libraryData?.items.length && (
                <Notice height={500}>내 서재에 등록된 책이 없습니다.</Notice>
              )}

              <List columns={2} sx={{ mt: 4 }}>
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
                        title={<>{item.title}</>}
                        content={item.authors.join(' · ')}
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
        </Container>
      </Main>
    </Layout>
  );
};
