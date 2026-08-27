import { useCallback } from 'react';
import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { useLoadData } from '@/services/core/useLoadData';
import { getHomePopularBooks } from '@/services/apis/homePopularBooks/repository';

import './IntroPage.css';
import './IntroInteraction.css';

const BOOKS_MAX_LENGTH = 7;

export const IntroPage = () => {
  const getHomePopularBooksLoadData = useCallback(async () => {
    return await getHomePopularBooks({});
  }, []);
  const {
    status: { data },
  } = useLoadData({
    queryFn: getHomePopularBooksLoadData,
  });

  const navigation = useNavigate();

  const handleClickMoveDetail = (isbn: string) => {
    navigation(`/books/${isbn}`);
  };

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
                    <div className="book-entry">
                      <a
                        className="book-float"
                        href="#"
                        onClick={(e: MouseEvent<HTMLAnchorElement>) => {
                          e.preventDefault();
                          handleClickMoveDetail(book.isbn13);
                        }}
                      >
                        <img src={book.coverImageUrl} alt="" />
                      </a>
                    </div>
                  </div>
                );
              })}
          </div>
        </div>
      </Main>
    </Layout>
  );
};
