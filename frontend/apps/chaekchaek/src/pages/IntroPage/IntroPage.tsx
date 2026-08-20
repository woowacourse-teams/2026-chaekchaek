import { useCallback, useEffect } from 'react';

import { Layout } from '@chaekchaek/design-system';
import { Header } from '@chaekchaek/design-system';
import { Main } from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { getHomePopularBooks } from '@/services/apis/homePopularBooks/repository';

import './IntroPage.css';
import { init } from './book-physics';

const BOOKS_MAX_LENGTH = 7;

export const IntroPage = () => {
  useEffect(() => {
    // init();
  }, []);

  const getHomePopularBooksLoadData = useCallback(async () => {
    return await getHomePopularBooks({});
  }, []);
  const {
    status: { data },
  } = useLoadData({
    queryFn: getHomePopularBooksLoadData,
  });

  return (
    <Layout>
      <Header />
      <Main>
        <div className="scene">
          <div data-pencil-name="다크 흩어진 책 웹 홈">
            {data?.books
              .filter((_, index) => index < BOOKS_MAX_LENGTH)
              .map((book, index) => {
                const bookIndex = index;

                return (
                  <div data-pencil-name={`다크 홈 전체 책 ${bookIndex}`} className="book">
                    <img src={book.coverImageUrl} alt="" />
                  </div>
                );
              })}
          </div>
        </div>
      </Main>
    </Layout>
  );
};
