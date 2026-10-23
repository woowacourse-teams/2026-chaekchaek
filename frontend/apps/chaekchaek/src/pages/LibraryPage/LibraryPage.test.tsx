import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';

import { describe, expect, it, vi } from 'vitest';

import html2canvas from 'html2canvas';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibraryPage } from '@/pages/LibraryPage';

import {
  addedBookResponse,
  bookSearchResponse,
  downloadedLibraryImageDataUrl,
  emptyBookSearchResponse,
  fullLibraryResponse,
  libraryAfterAddResponse,
  libraryResponse,
  myLibraryResponse,
  renamedLibraryName,
  searchedBook,
} from './LibraryPage.fixtures';

vi.mock('html2canvas', () => ({ default: vi.fn() }));

describe('LibraryPage', () => {
  it('책장에 진입하면 등록된 책장 내 책 목록이 표시된다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(libraryResponse));
      }),
    );

    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    for (const book of libraryResponse.items) {
      expect(await screen.findByText(book.title)).toBeVisible();
    }
  });

  it('책장에 등록된 모든 책의 책등이 표시된다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${fullLibraryResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(fullLibraryResponse));
      }),
    );

    const { container } = renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    await waitFor(() => {
      // 책등 이미지의 alt가 비어 있어 img 역할 대신 응답의 이미지 주소로 조회한다.
      const images = Array.from(container.querySelectorAll('img'));

      // 한 번의 응답에 포함된 23권의 책등이 모두 함께 보여야 한다.
      for (const book of fullLibraryResponse.items) {
        const spineImage = images.find((image) => image.getAttribute('src') === book.spineImageUrl);

        expect(spineImage, `${book.title} 책등 이미지`).toBeDefined();
        expect(spineImage).toBeVisible();
      }
    });
  });

  it('책장 이름을 변경하면 변경된 이름이 표시된다', async () => {
    const user = userEvent.setup();
    const currentLibrary = structuredClone(libraryResponse);

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(currentLibrary));
      }),
      http.patch(
        `${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`,
        async ({ request }) => {
          const { name } = (await request.json()) as { name: string };
          currentLibrary.name = name;

          return new HttpResponse(null, { status: 204 });
        },
      ),
    );

    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    const libraryName = await screen.findByText(libraryResponse.name);
    expect(libraryName).toBeVisible();

    // 더보기 버튼에 접근 가능한 이름이 없어 책장 제목 영역으로 범위를 제한한다.
    const libraryTitle = libraryName.closest('.ui-Title');
    expect(libraryTitle).not.toBeNull();
    await user.click(within(libraryTitle as HTMLElement).getByRole('button'));

    const renameButton = screen.getByRole('button', { name: '책장 이름 바꾸기' });
    expect(renameButton).toBeVisible();
    await user.click(renameButton);

    const renameTitle = screen.getByText('책장 이름 바꾸기');
    expect(renameTitle).toBeVisible();
    // 팝업에 dialog 역할이 없어 제목으로 팝업 영역을 찾아 입력 필드와 버튼을 조회한다.
    const renameDialog = renameTitle.closest('.ui-Dialog-Container');
    expect(renameDialog).not.toBeNull();
    const popup = within(renameDialog as HTMLElement);
    const nameInput = popup.getByRole('textbox');
    expect(nameInput).toBeVisible();
    await user.clear(nameInput);
    await user.type(nameInput, renamedLibraryName);
    await user.click(popup.getByRole('button', { name: '저장' }));

    expect(await screen.findByText(renamedLibraryName)).toBeVisible();
  });

  it('이미지 저장 시 현재 책장의 책등 이미지가 다운로드된다', async () => {
    const user = userEvent.setup();
    const canvas = document.createElement('canvas');
    const toDataURL = vi.spyOn(canvas, 'toDataURL').mockReturnValue(downloadedLibraryImageDataUrl);
    vi.mocked(html2canvas).mockResolvedValue(canvas);

    // jsdom에서는 실제 파일을 저장할 수 없어 다운로드 링크 클릭 시의 값을 확인한다.
    const downloads: { href: string; filename: string }[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      downloads.push({ href: this.href, filename: this.download });
    });

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(libraryResponse));
      }),
    );

    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    const libraryName = await screen.findByText(libraryResponse.name);
    const libraryTitle = libraryName.closest('.ui-Title');
    expect(libraryTitle).not.toBeNull();
    await user.click(within(libraryTitle as HTMLElement).getByRole('button'));
    await user.click(screen.getByRole('button', { name: '이미지 저장' }));

    const imageDialogTitle = screen.getByText('책장을 이미지로');
    expect(imageDialogTitle).toBeVisible();
    const imageDialog = imageDialogTitle.closest('.ui-Dialog-Container');
    expect(imageDialog).not.toBeNull();
    await user.click(
      within(imageDialog as HTMLElement).getByRole('button', { name: '이미지 저장' }),
    );

    await waitFor(() => {
      expect(downloads).toEqual([
        { href: downloadedLibraryImageDataUrl, filename: expect.stringMatching(/\.png$/) },
      ]);
    });
    expect(toDataURL).toHaveBeenCalledWith('image/png');
    expect(html2canvas).toHaveBeenCalledTimes(1);

    // 페이지의 이미지가 아니라 실제 다운로드용으로 캡처한 영역의 책등을 검증한다.
    const captureCall = vi.mocked(html2canvas).mock.calls[0];
    if (!captureCall) {
      throw new Error('책등 이미지 캡처가 호출되지 않았습니다.');
    }

    const [capturedElement] = captureCall;
    const capturedSpines = Array.from(capturedElement.querySelectorAll('img')).map((image) =>
      image.getAttribute('src'),
    );
    expect(capturedSpines).toEqual(libraryResponse.items.map((book) => book.spineImageUrl));
  });

  it('내 서재에서 탭에서 검색한 책을 선택하여 책장에 추가할 수 있다', async () => {
    const user = userEvent.setup();
    let isBookAdded = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(
          structuredClone(isBookAdded ? libraryAfterAddResponse : libraryResponse),
        );
      }),
      http.get(`${ENV.APP_API_URL}/api/v1/library`, () => {
        return HttpResponse.json(structuredClone(myLibraryResponse));
      }),
      http.post(
        `${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`,
        async ({ request }) => {
          const { isbn13 } = (await request.json()) as { isbn13: string };
          if (isbn13 !== searchedBook.isbn13) {
            return new HttpResponse(null, { status: 400 });
          }
          isBookAdded = true;
          return HttpResponse.json(structuredClone(addedBookResponse), { status: 201 });
        },
      ),
    );

    const { container } = renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    expect(await screen.findByText(libraryResponse.name)).toBeVisible();
    // 본문에 main 역할이 없어 클래스 이름으로 범위를 제한한다.
    const libraryContent = container.querySelector<HTMLElement>('.frame-Main');
    if (!libraryContent) throw new Error('책장 본문을 찾을 수 없습니다.');
    const library = within(libraryContent);
    expect(library.queryByText(searchedBook.title)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '책 담기' }));

    const dialogTitle = screen.getByText('책장에 책 담기');
    const dialog = dialogTitle.closest<HTMLElement>('.ui-Dialog-Container');
    if (!dialog) throw new Error('책 담기 팝업을 찾을 수 없습니다.');
    const popup = within(dialog);
    await user.click(popup.getByText('내 서재에서'));
    await user.type(
      popup.getByPlaceholderText('책 제목이나 작가를 검색하세요'),
      searchedBook.title,
    );

    const searchResult = await popup.findByText(searchedBook.title);
    expect(searchResult).toBeVisible();
    const bookItem = searchResult.closest<HTMLElement>('.ui-List-Item');
    if (!bookItem) throw new Error('검색한 책의 목록 항목을 찾을 수 없습니다.');
    const checkbox = within(bookItem).getByRole('checkbox');
    await user.click(checkbox);
    expect(checkbox).toBeChecked();
    await user.click(popup.getByRole('button', { name: '담기' }));

    // 검색 결과가 아닌 현재 책장의 본문에 추가되었는지 확인한다.
    expect(await library.findByText(searchedBook.title)).toBeVisible();
  });

  it('책 검색 탭에서 검색한 책을 선택하여 책장에 추가할 수 있다', async () => {
    const user = userEvent.setup();
    let isBookAdded = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(
          structuredClone(isBookAdded ? libraryAfterAddResponse : libraryResponse),
        );
      }),
      http.get(`${ENV.APP_API_URL}/api/v1/books`, ({ request }) => {
        const query = new URL(request.url).searchParams.get('query');
        return HttpResponse.json(
          structuredClone(
            query === searchedBook.title ? bookSearchResponse : emptyBookSearchResponse,
          ),
        );
      }),
      http.post(
        `${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`,
        async ({ request }) => {
          const { isbn13 } = (await request.json()) as { isbn13: string };
          if (isbn13 !== searchedBook.isbn13) {
            return new HttpResponse(null, { status: 400 });
          }
          isBookAdded = true;
          return HttpResponse.json(structuredClone(addedBookResponse), { status: 201 });
        },
      ),
    );

    // jsdom에 없는 스크롤 API만 대체하고 테스트가 끝나면 원래 상태로 복구한다.
    const scrollToDescriptor = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollTo');
    Object.defineProperty(HTMLElement.prototype, 'scrollTo', {
      configurable: true,
      value: vi.fn(),
    });

    try {
      const { container } = renderProvider(
        <Routes>
          <Route path="/libraries/:libraryId" element={<LibraryPage />} />
        </Routes>,
        {
          auth: defaultLoggedAuthContextValue,
          initialEntries: [`/libraries/${libraryResponse.libraryId}`],
        },
      );

      expect(await screen.findByText(libraryResponse.name)).toBeVisible();
      // 본문에 main 역할이 없어 클래스 이름으로 범위를 제한한다.
      const libraryContent = container.querySelector<HTMLElement>('.frame-Main');
      if (!libraryContent) throw new Error('책장 본문을 찾을 수 없습니다.');
      const library = within(libraryContent);
      expect(library.queryByText(searchedBook.title)).not.toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: '책 담기' }));

      const dialogTitle = screen.getByText('책장에 책 담기');
      const dialog = dialogTitle.closest<HTMLElement>('.ui-Dialog-Container');
      if (!dialog) throw new Error('책 담기 팝업을 찾을 수 없습니다.');
      const popup = within(dialog);
      await user.click(popup.getByText('책 검색'));
      await user.type(
        popup.getByPlaceholderText('책 제목이나 작가를 검색하세요'),
        searchedBook.title,
      );
      const searchResult = await popup.findByText(searchedBook.title);
      expect(searchResult).toBeVisible();
      const bookItem = searchResult.closest<HTMLElement>('.ui-List-Item');
      if (!bookItem) throw new Error('검색한 책의 목록 항목을 찾을 수 없습니다.');
      const checkbox = within(bookItem).getByRole('checkbox');
      await user.click(checkbox);
      expect(checkbox).toBeChecked();
      await user.click(popup.getByRole('button', { name: '담기' }));

      // 팝업에 남아 있는 검색 결과로 테스트가 통과하지 않도록 책장 본문에서 확인한다.
      expect(await library.findByText(searchedBook.title)).toBeVisible();
    } finally {
      if (scrollToDescriptor) {
        Object.defineProperty(HTMLElement.prototype, 'scrollTo', scrollToDescriptor);
      } else {
        Reflect.deleteProperty(HTMLElement.prototype, 'scrollTo');
      }
    }
  });
});
