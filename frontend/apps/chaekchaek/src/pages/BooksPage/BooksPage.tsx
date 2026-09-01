import { useCallback, useEffect, useState } from 'react';
import type { ChangeEvent } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { Icon, Notice, Split } from '@chaekchaek/design-system';
import { Title } from '@chaekchaek/design-system';
import { List } from '@chaekchaek/design-system';
import { ImgBox } from '@chaekchaek/design-system';
// import DummyImgImgBox from '../../ImgBox/imgs/dummy.png';
import { Button } from '@chaekchaek/design-system';
import { Input } from '@chaekchaek/design-system';
import { Pagination } from '@chaekchaek/design-system';
import { Badge } from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { getBooks } from '@/services/apis/books/repository';
import { postLibrary } from '@/services/apis/library/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { useExecute } from '@/services/core/useExecute';

import { LoginDialog } from '@/pages/LoginPage/dialog/LoginDialog';

export const BooksPage = () => {
  const { isAuthenticated } = useAuthContext();

  const [openLoginDialog, setOpenLoginDialog] = useState(false);
  const handleOpenLoginDialog = () => {
    if (!isAuthenticated) setOpenLoginDialog(true);
  };
  const handleCloseLoginDialog = () => {
    setOpenLoginDialog(false);
  };

  const [searchParams, setSearchParams] = useSearchParams();
  const keywordQuery = searchParams.get('query') ?? '';
  const defaultPage = searchParams.get('page') ? Number(searchParams.get('page')) : 1;

  const [query, setQuery] = useState(() => keywordQuery ?? '');
  const [page, setPage] = useState(() => Number(defaultPage) ?? 1);

  const handleChangeQuery = (e: ChangeEvent<HTMLInputElement>) => {
    const { value } = e.target;
    setQuery(value);
    setPage(1);

    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set('page', '1');
        next.set('query', value);
        return next;
      },
      { replace: true },
    );
  };

  const handleChangeDefaultPage = (defaultPage: number) => {
    setPage(defaultPage);

    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set('page', String(defaultPage));
        return next;
      },
      { replace: true },
    );
  };

  const getBooksLoadData = useCallback(async () => {
    if (!keywordQuery.length) return null;
    return await getBooks({ page: defaultPage, query: keywordQuery });
  }, [keywordQuery, defaultPage]);

  const {
    status: { data },
  } = useLoadData({
    queryFn: getBooksLoadData,
  });

  const totalPages = data ? Math.ceil(data.totalCount / 10) : 1;

  const navigation = useNavigate();
  const handleMove = (isbn: string) => {
    track('select_book', {
      source: 'search',
    });

    navigation(`/books/${isbn}`);
  };

  const { mutate } = useExecute({
    executeFn: postLibrary,
  });
  const handleRegisterLibrary = async (isbn: string) => {
    if (!isAuthenticated) return handleOpenLoginDialog();

    await mutate({ isbn13: isbn, status: 'WANT_TO_READ' });

    track('library_add', {
      source: 'search',
      status: 'want_to_read',
    });

    handleMove(isbn);
  };

  useEffect(() => {
    if (!query.length) return;

    const TIMEOUT = 500;
    const timeoutId = setTimeout(() => {
      track('search');
    }, TIMEOUT);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [query]);

  return (
    <Layout>
      <Header />
      <Main>
        <Split>
          <Split.Side>
            <Title
              level="page"
              orientation="vertical"
              trailing={
                <>
                  <Input
                    block
                    leading={<Icon.SearchIcon />}
                    autoFocus
                    value={query}
                    onChange={handleChangeQuery}
                  />
                </>
              }
            >
              책 찾기
            </Title>
          </Split.Side>
          <Split.Content>
            <Title level="main" trailing={<></>}>
              '{keywordQuery}' 검색 결과
            </Title>
            {data && !data?.items?.length && (
              <Notice height={500}>검색된 데이터가 없습니다.</Notice>
            )}
            <List>
              {!!data?.items.length &&
                data?.items.map((item) => {
                  return (
                    <List.Item>
                      <List.Item.Leading>
                        <Link
                          to={`/books/${item.isbn13}`}
                          onClick={() => {
                            handleMove(item.isbn13);
                          }}
                        >
                          <ImgBox img={item.coverImageUrl} size="small" />
                        </Link>
                      </List.Item.Leading>
                      <List.Item.Content
                        as={Link}
                        to={`/books/${item.isbn13}`}
                        onClick={() => {
                          handleMove(item.isbn13);
                        }}
                        title={item.title}
                        content={item.authors.join(' · ')}
                        description={`${item.publisher} · ${item.publishedDate}`}
                      />
                      <List.Item.Trailing>
                        {(item.reviewCount || item.replyCount) && (
                          <Badge variant="subtle" size="small">
                            감상 {item.reviewCount || 0} · 답글 {item.replyCount || 0}
                          </Badge>
                        )}
                        {!item.isRegisteredInMyLibrary && (
                          <Button
                            variant="primary"
                            onClick={() => {
                              handleRegisterLibrary(item?.isbn13);
                            }}
                          >
                            내 서재 담기
                          </Button>
                        )}
                      </List.Item.Trailing>
                    </List.Item>
                  );
                })}
            </List>
            {data && (
              <Pagination
                sx={{ mt: 5 }}
                defaultPage={defaultPage}
                totalPages={totalPages}
                onChange={handleChangeDefaultPage}
              />
            )}
          </Split.Content>
        </Split>

        {openLoginDialog && <LoginDialog onClose={handleCloseLoginDialog} />}
      </Main>
    </Layout>
  );
};
