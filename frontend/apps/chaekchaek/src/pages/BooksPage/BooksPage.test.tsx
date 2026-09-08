import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { describe, it, expect } from 'vitest';

import { server } from '@/mocks/msw/server';
import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';

import { renderProvider } from '@/test/utils/render';

import { BooksPage } from './BooksPage';

import { harrySearchPage1, martianSearchPage } from './BooksPage.fixtures';

describe('BooksPage', () => {
  it('기본 렌더링이 된다', () => {
    renderProvider(<BooksPage />);

    expect(screen.getByText('책 찾기')).toBeInTheDocument();
  });

  it('검색어를 입력하면 검색 결과를 보여준다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => {
        return HttpResponse.json({
          ...harrySearchPage1,
        });
      }),
    );

    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');

    expect(await screen.findByText(/개소리에/)).toBeInTheDocument();

    expect(await screen.findByText(/^해리 포터와 마법사의 돌 1$/)).toBeInTheDocument();
  });

  it('초기 url 주소에 검색어가 있으면 그 검색어로 검색 결과를 보여준다', async () => {
    const searchKeyword = '마션';

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, ({ request }) => {
        const url = new URL(request.url);

        expect(url.searchParams.get('query')).toBe(searchKeyword);

        return HttpResponse.json({
          ...martianSearchPage,
        });
      }),
    );

    renderProvider(<BooksPage />, { initialEntries: [`/books?query=${searchKeyword}`] });

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), searchKeyword);

    expect(await screen.findByText(/마션/)).toBeInTheDocument();
  });

  it('검색어가 변경되면 1페이지로 초기화한다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => {
        return HttpResponse.json({
          ...harrySearchPage1,
        });
      }),
    );

    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    const searchInput = screen.getByRole('textbox', { name: '책 검색' });

    await user.type(searchInput, '해리');
    await user.click(await screen.findByRole('button', { name: /Page 2/i }));

    expect(await screen.findByRole('button', { name: 'Page 2' })).toHaveAttribute(
      'aria-current',
      'page',
    );

    await user.clear(searchInput);
    await user.type(searchInput, '마션');

    expect(await screen.findByRole('button', { name: 'Page 1' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });
});
