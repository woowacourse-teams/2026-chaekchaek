import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { describe, it, expect, vi } from 'vitest';

import { server } from '@/mocks/msw/server';
import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { BooksPage } from './BooksPage';

import { harrySearchPage1, harrySearchPage2, martianSearchPage } from './BooksPage.fixtures';

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

  it('2페이지를 검색하면 2페이지의 검색 결과를 보여준다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, ({ request }) => {
        const url = new URL(request.url);

        return HttpResponse.json(
          url.searchParams.get('page') === '2' ? harrySearchPage2 : harrySearchPage1,
        );
      }),
    );

    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');

    expect(await screen.findByText('개소리에 대하여')).toBeInTheDocument();
    expect(screen.queryByText('해리 포터 5~7부 세트')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Page 2' }));

    expect(await screen.findByText('해리 포터 5~7부 세트')).toBeInTheDocument();
    expect(screen.queryByText('개소리에 대하여')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Page 2' })).toHaveAttribute('aria-current', 'page');
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

  it('로그인 사용자가 내서재에 안 넣는 책의 경우 버튼이 나타난다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => {
        return HttpResponse.json(harrySearchPage1);
      }),
    );

    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');

    const title = await screen.findByText('개소리에 대하여');
    const bookItem = title.closest('li');

    expect(bookItem).not.toBeNull();
    expect(within(bookItem!).getByRole('button', { name: '내 서재 담기' })).toBeInTheDocument();
  });

  it('로그인 사용자가 내서재에 넣는 책의 경우 버튼이 안 나타난다', async () => {
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

    const title = screen.getByText(/^해리 포터와 마법사의 돌 1$/);
    const bookItem = title.closest('li');

    expect(bookItem).not.toBeNull();

    expect(
      within(bookItem!).queryByRole('button', { name: '내서재에 넣기' }),
    ).not.toBeInTheDocument();
  });

  it('로그인 사용자가 내서재에 넣기 버튼 클릭시 내서재에 담긴다', async () => {
    const requestListener = vi.fn();

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => {
        return HttpResponse.json({
          ...harrySearchPage1,
          items: harrySearchPage1.items.map((item) => ({
            ...item,
            isRegisteredInMyLibrary: false,
          })),
        });
      }),
    );

    server.events.on('request:start', async ({ request }) => {
      const url = new URL(request.url);

      if (request.method === 'POST' && url.pathname === '/api/v1/library') {
        const body = await request.clone().json();
        console.log(body);
        requestListener(body);
      }
    });

    renderProvider(<BooksPage />, { auth: defaultLoggedAuthContextValue });

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');

    const book = {
      title: '개소리에 대하여',
      isbn: '9791157833870',
    };

    const title = await screen.findByText(book.title);
    const bookItem = title.closest('li');

    expect(bookItem).not.toBeNull();

    await user.click(within(bookItem!).getByRole('button', { name: '내 서재 담기' }));

    expect(requestListener).toHaveBeenCalled();
    expect(requestListener).toHaveBeenCalledWith({
      isbn13: book.isbn,
      status: 'WANT_TO_READ',
    });
  });

  it('비로그인 사용자가 내서재에 넣기 버튼 클릭시 로그인 팝업이 뜬다', async () => {
    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');

    await user.click(screen.getByRole('button', { name: '내 서재 담기' }));

    expect(screen.getByText(/로 시작하기/)).toBeInTheDocument();
  });
});
