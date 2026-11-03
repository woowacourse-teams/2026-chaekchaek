import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { describe, it, expect } from 'vitest';

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
    const searchInput = screen.getByRole('textbox', { name: '책 검색' });

    await user.clear(searchInput);
    await user.type(searchInput, searchKeyword);

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

  it('하나 이상의 책장에 담긴 책은 담았어요 버튼을 표시한다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => HttpResponse.json(harrySearchPage1)),
    );

    renderProvider(<BooksPage />, { auth: defaultLoggedAuthContextValue });
    const user = userEvent.setup();

    // 책 제목을 검색한다.
    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');
    const bookTitle = await screen.findByText('해리 포터와 마법사의 돌 1');

    // 검색 결과에서 책장에 담긴 책에 담았어요 버튼이 표시되는지 확인한다.
    const book = bookTitle.closest('li');
    expect(book).not.toBeNull();
    expect(within(book!).getByRole('button', { name: '담았어요' })).toBeVisible();
  });

  it('어떤 책장에도 담기지 않은 책은 내 서재에 담기 버튼을 표시한다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => HttpResponse.json(harrySearchPage1)),
    );

    renderProvider(<BooksPage />, { auth: defaultLoggedAuthContextValue });
    const user = userEvent.setup();

    // 책 제목을 검색한다.
    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');
    const bookTitle = await screen.findByText('개소리에 대하여');

    // 검색 결과에서 어떤 책장에도 담기지 않은 책에 내 서재에 담기 버튼이 표시되는지 확인한다.
    const book = bookTitle.closest('li');
    expect(book).not.toBeNull();
    expect(within(book!).getByRole('button', { name: '내 서재에 담기' })).toBeVisible();
  });

  it('로그인 사용자가 내 서재에 담기 버튼 클릭하면 담을 책장 고르기 팝업이 나타난다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => HttpResponse.json(harrySearchPage1)),
    );

    renderProvider(<BooksPage />, { auth: defaultLoggedAuthContextValue });
    const user = userEvent.setup();

    // 책 제목을 검색한다.
    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');
    const bookTitle = await screen.findByText('개소리에 대하여');

    // 검색 결과에서 책의 내 서재에 담기 버튼을 클릭한다.
    const book = bookTitle.closest('li');
    expect(book).not.toBeNull();
    await user.click(within(book!).getByRole('button', { name: '내 서재에 담기' }));

    // 담을 책장 고르기 팝업의 안내 문구, 책장 선택 항목과 버튼이 표시되는지 확인한다.
    expect(await screen.findByText('담을 책장 고르기')).toBeVisible();
    expect(screen.getByText('최근 담은 책장이 선택되어 있어요.')).toBeVisible();
    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes.length).toBeGreaterThan(0);
    checkboxes.forEach((checkbox) => expect(checkbox).toBeVisible());
    expect(screen.getByRole('button', { name: '닫기' })).toBeVisible();
    expect(screen.getByRole('button', { name: '이 책장에 담기' })).toBeVisible();
  });

  it('비로그인 사용자가 내 서재에 담기 버튼 클릭시 로그인 팝업이 뜬다', async () => {
    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');

    await user.click(screen.getByRole('button', { name: '내 서재에 담기' }));

    expect(screen.getByText(/로 로그인/)).toBeInTheDocument();
  });
});
