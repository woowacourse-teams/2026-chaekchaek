import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';

import { describe, it, expect, vi } from 'vitest';

import { server } from '@/mocks/msw/server';
import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibraryPage } from '@/pages/LibraryPage';

import { BooksPage } from './BooksPage';

import {
  bookRegistrationSearchResponse,
  bookToRegister,
  harrySearchPage1,
  harrySearchPage2,
  librariesBeforeBookRegistrationResponse,
  libraryAfterBookRegistrationResponse,
  martianSearchPage,
  registeredBookResponse,
  unselectedLibraryResponse,
} from './BooksPage.fixtures';

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

  it('로그인 사용자가 내 서재 담기 버튼 클릭하면 책장 선택하기 팝업이 나타난다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => HttpResponse.json(harrySearchPage1)),
    );

    renderProvider(<BooksPage />, { auth: defaultLoggedAuthContextValue });
    const user = userEvent.setup();

    // 책 제목을 검색한다.
    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');
    const bookTitle = await screen.findByText('개소리에 대하여');

    // 검색 결과에서 책의 내 서재 담기 버튼을 클릭한다.
    const book = bookTitle.closest('li');
    expect(book).not.toBeNull();
    await user.click(within(book!).getByRole('button', { name: '내 서재 담기' }));

    // 책장을 선택할 수 있는 팝업과 담기 버튼이 표시되는지 확인한다.
    expect(await screen.findByText('저장할 책장 고르기')).toBeVisible();
    expect(screen.getByText('저장할 책장 하나 이상을 선택해 주세요.')).toBeVisible();
    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes.length).toBeGreaterThan(0);
    checkboxes.forEach((checkbox) => expect(checkbox).toBeVisible());
    expect(screen.getByRole('button', { name: '담기' })).toBeVisible();
  });

  it('선택하지 않았던 책장을 선택하고 완료하면 해당 책장에 책이 등록된다', async () => {
    const user = userEvent.setup();
    let isBookRegistered = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/books`, () => {
        return HttpResponse.json(structuredClone(bookRegistrationSearchResponse));
      }),
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(librariesBeforeBookRegistrationResponse));
      }),
      http.post(
        `${ENV.APP_API_URL}/api/v1/libraries/${unselectedLibraryResponse.libraryId}`,
        async ({ request }) => {
          const { isbn13 } = (await request.json()) as { isbn13: string };
          if (isbn13 !== bookToRegister.isbn13) {
            return new HttpResponse(null, { status: 400 });
          }
          isBookRegistered = true;
          return HttpResponse.json(structuredClone(registeredBookResponse), { status: 201 });
        },
      ),
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${unselectedLibraryResponse.libraryId}`, () => {
        return HttpResponse.json(
          structuredClone(
            isBookRegistered ? libraryAfterBookRegistrationResponse : unselectedLibraryResponse,
          ),
        );
      }),
    );

    const { unmount } = renderProvider(<BooksPage />, { auth: defaultLoggedAuthContextValue });

    // 책 제목을 검색한다.
    await user.type(screen.getByRole('textbox', { name: '책 검색' }), bookToRegister.title);
    const bookTitle = await screen.findByText(bookToRegister.title);

    // 검색 결과에서 책의 내 서재 담기 버튼을 클릭한다.
    const bookItem = bookTitle.closest('li');
    if (!bookItem) throw new Error('검색한 책의 목록 항목을 찾을 수 없습니다.');
    await user.click(within(bookItem).getByRole('button', { name: '내 서재 담기' }));

    // 책장 선택 팝업에서 아직 선택하지 않은 책장을 확인한다.
    const libraryName = await screen.findByText(unselectedLibraryResponse.name);
    const libraryItem = libraryName.closest('li');
    if (!libraryItem) throw new Error('선택할 책장의 목록 항목을 찾을 수 없습니다.');
    const checkbox = within(libraryItem).getByRole('checkbox');
    expect(checkbox).not.toBeChecked();

    // 책장을 선택하고 선택 상태가 표시되는지 확인한다.
    await user.click(checkbox);
    expect(checkbox).toBeChecked();

    // 담기 버튼을 눌러 선택을 완료하고 팝업이 닫히는지 확인한다.
    await user.click(screen.getByRole('button', { name: '담기' }));
    await waitFor(() => {
      expect(screen.queryByText('저장할 책장 고르기')).not.toBeInTheDocument();
    });

    // 선택한 책장 페이지에 방문한다.
    unmount();
    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${unselectedLibraryResponse.libraryId}`],
      },
    );

    // 선택한 책장에 등록한 책이 표시되는지 확인한다.
    expect(await screen.findByText(unselectedLibraryResponse.name)).toBeVisible();
    expect(await screen.findByText(bookToRegister.title)).toBeVisible();
  });

  it('비로그인 사용자가 내서재에 넣기 버튼 클릭시 로그인 팝업이 뜬다', async () => {
    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '해리');

    await user.click(screen.getByRole('button', { name: '내 서재 담기' }));

    expect(screen.getByText(/로 로그인/)).toBeInTheDocument();
  });
});
