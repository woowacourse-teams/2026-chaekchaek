import { useCallback, useState } from 'react';
import type { ChangeEvent, MouseEvent } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { Split } from '@chaekchaek/design-system';
import { OptionList } from '@chaekchaek/design-system';
import { Title } from '@chaekchaek/design-system';
import { List } from '@chaekchaek/design-system';
import { ImgBox } from '@chaekchaek/design-system';
// import DummyImgImgBox from '../../ImgBox/imgs/dummy.png';
import { Button } from '@chaekchaek/design-system';
import { Input } from '@chaekchaek/design-system';
import { Pagination } from '@chaekchaek/design-system';
import { Badge } from '@chaekchaek/design-system';

import { getBooks } from '@/services/apis/books/repository';
import { postLibrary } from '@/services/apis/library/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { useExecute } from '@/services/core/useExecute';

export const BooksPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const keywordQuery = searchParams.get('query') ?? '';
  const defaultPage = searchParams.get('page') ? Number(searchParams.get('page')) : 1;

  const [query, setQuery] = useState(() => keywordQuery ?? '');
  const [page, setPage] = useState(() => Number(defaultPage) ?? 1);

  const handleChangeQuery = (e: ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
    setPage(1);

    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('query', query);
      return next;
    });
  };

  const handleChangeDefaultPage = (defaultPage: number) => {
    setPage(defaultPage);

    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', String(defaultPage));
      return next;
    });
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

  const navigation = useNavigate();
  const handleMove = (isbn: string) => {
    navigation(`/books/${isbn}`);
  };

  const { mutate } = useExecute({
    executeFn: postLibrary,
  });
  const handleRegisterLibrary = async (isbn: string) => {
    await mutate({ isbn13: isbn, status: 'READING' });
  };

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
                  <Input block value={query} onChange={handleChangeQuery} />
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
            <List>
              {!!data?.items.length &&
                data?.items.map((item) => {
                  return (
                    <List.Item>
                      <List.Item.Leading>
                        <a
                          href="#"
                          onClick={(e: MouseEvent<HTMLAnchorElement>) => {
                            e.preventDefault();

                            handleMove(item.isbn13);
                          }}
                        >
                          <ImgBox img={item.coverImageUrl} size="small" />
                        </a>
                      </List.Item.Leading>
                      <List.Item.Content
                        title={item.title}
                        content={item.authors.join(' · ')}
                        description={`${item.publisher} · ${item.publishedDate}`}
                        onClick={() => {
                          handleMove(item.isbn13);
                        }}
                      />
                      <List.Item.Trailing>
                        {(item.reviewCount || item.replyCount) && (
                          <Badge variant="subtle" size="small">
                            감상 {item.reviewCount || 0} · 답글 {item.replyCount || 0}
                          </Badge>
                        )}
                        <Button
                          variant="primary"
                          onClick={() => {
                            handleRegisterLibrary(item?.isbn13);
                          }}
                        >
                          읽는 중 시작
                        </Button>
                      </List.Item.Trailing>
                    </List.Item>
                  );
                })}
            </List>
            {data && (
              <Pagination
                defaultPage={page}
                totalPages={data?.totalCount}
                onChange={handleChangeDefaultPage}
              />
            )}
          </Split.Content>
        </Split>
      </Main>
    </Layout>
  );
};
