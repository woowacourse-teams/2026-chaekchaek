import { useCallback } from 'react';
import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { track } from '@/analytics/track';

import { useLoadData } from '@/services/core/useLoadData';
import { getHomePopularBooks } from '@/services/apis/homePopularBooks/repository';

import './PopularBooks.css';
import './IntroInteraction.css';

export const PopularBooks = () => {
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
    track('navigate', {
      destination: 'book_detail',
      source: 'intro_popular',
    });

    navigation(`/books/${isbn}`);
  };

  return (
    <div className="scene">
      <div className="popular-books-title">지금 책책에서 인기 있는 책</div>
      <div className="popular-books">
        {data?.books.map((book) => {
          return (
            <div className="book" key={book.isbn13}>
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
  );
};
