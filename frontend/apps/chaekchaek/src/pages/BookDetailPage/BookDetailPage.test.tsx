import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';

import { describe, it, expect } from 'vitest';

import { server } from '@/mocks/msw/server';
import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { BookDetailPage } from './BookDetailPage';
import {
  bookInLibraryResponse,
  bookNotInLibraryResponse,
  emptyBookReviewsResponse,
} from './BookDetailPage.fixtures';

describe('BookDetailPage', () => {
  it.each([
    { ratingCount: 0, averageRating: 0, label: '별점 없음' },
    { ratingCount: 2, averageRating: 4, label: '★ 0 / 4' },
    { ratingCount: 2, averageRating: 0, label: '★ 0 / 0' },
    { ratingCount: null, averageRating: 0, label: '★ 0 / 0' },
  ])(
    '별점 수 $ratingCount, 평균 $averageRating일 때 $label을 표시한다',
    async ({ ratingCount, averageRating, label }) => {
      server.use(
        http.get(`${ENV.APP_API_URL}/api/v2/books/by-isbn/${bookNotInLibraryResponse.isbn13}`, () =>
          HttpResponse.json({ ...bookNotInLibraryResponse, ratingCount, averageRating }),
        ),
        http.get(`${ENV.APP_API_URL}/api/v1/books/${bookNotInLibraryResponse.bookId}/reviews`, () =>
          HttpResponse.json(structuredClone(emptyBookReviewsResponse)),
        ),
      );
      renderProvider(
        <Routes>
          <Route path="/books/:isbn" element={<BookDetailPage />} />
        </Routes>,
        { initialEntries: [`/books/${bookNotInLibraryResponse.isbn13}`] },
      );

      await screen.findByText(bookNotInLibraryResponse.title);
      expect(screen.getByText(label)).toBeVisible();
      if (ratingCount !== 0) {
        expect(screen.queryByText('별점 없음')).not.toBeInTheDocument();
      }
    },
  );

  it('책이 책장에 담겨 있지 않은 경우 내 서재에 담기 버튼이 보인다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v2/books/by-isbn/${bookNotInLibraryResponse.isbn13}`, () =>
        HttpResponse.json(structuredClone(bookNotInLibraryResponse)),
      ),
      http.get(`${ENV.APP_API_URL}/api/v1/books/${bookNotInLibraryResponse.bookId}/reviews`, () =>
        HttpResponse.json(structuredClone(emptyBookReviewsResponse)),
      ),
    );

    // 책장에 담지 않은 책의 상세 페이지에 방문한다.
    renderProvider(
      <Routes>
        <Route path="/books/:isbn" element={<BookDetailPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/books/${bookNotInLibraryResponse.isbn13}`],
      },
    );

    // 책 상세 정보와 내 서재에 담기 버튼이 표시되는지 확인한다.
    expect(await screen.findByText(bookNotInLibraryResponse.title)).toBeVisible();
    expect(await screen.findByRole('button', { name: '내 서재에 담기' })).toBeVisible();
  });

  it('책이 책장에 담겨 있는 경우 담았어요 버튼이 보인다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v2/books/by-isbn/${bookInLibraryResponse.isbn13}`, () =>
        HttpResponse.json(structuredClone(bookInLibraryResponse)),
      ),
      http.get(`${ENV.APP_API_URL}/api/v1/books/${bookInLibraryResponse.bookId}/reviews`, () =>
        HttpResponse.json(structuredClone(emptyBookReviewsResponse)),
      ),
    );

    // 책장에 담은 책의 상세 페이지에 방문한다.
    renderProvider(
      <Routes>
        <Route path="/books/:isbn" element={<BookDetailPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/books/${bookInLibraryResponse.isbn13}`],
      },
    );

    // 책 상세 정보와 담았어요 버튼이 표시되는지 확인한다.
    expect(await screen.findByText(bookInLibraryResponse.title)).toBeVisible();
    expect(await screen.findByRole('button', { name: '담았어요' })).toBeVisible();
  });
});
