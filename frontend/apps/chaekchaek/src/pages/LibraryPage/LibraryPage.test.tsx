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
import { LibrariesPage } from '@/pages/LibrariesPage';

import {
  addedBookResponse,
  bookSearchResponse,
  downloadedLibraryImageDataUrl,
  deletableLibraryResponse,
  librariesAfterDeleteResponse,
  emptyBookSearchResponse,
  fullLibraryResponse,
  libraryAfterAddResponse,
  libraryAfterBookDeleteResponse,
  bookToDelete,
  bookToKeep,
  libraryResponse,
  myLibraryResponse,
  librariesBeforeRenameResponse,
  otherLibraryResponse,
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

    // 책장 페이지에 진입한다.
    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 책장에 등록된 책 제목이 모두 표시되는지 확인한다.
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

    // 책장 페이지에 진입한다.
    const { container } = renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 책장에 등록된 모든 책의 책등 이미지가 표시되는지 확인한다.
    await waitFor(() => {
      const images = Array.from(container.querySelectorAll('img'));

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

    // 책장 페이지에 진입한다.
    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 현재 책장 이름이 표시되는지 확인한다.
    const libraryName = await screen.findByText(libraryResponse.name);
    expect(libraryName).toBeVisible();

    // 책장 제목 옆의 더보기 버튼을 클릭한다.
    const libraryTitle = libraryName.closest('.ui-Title');
    expect(libraryTitle).not.toBeNull();
    await user.click(within(libraryTitle as HTMLElement).getByRole('button', { name: '책장 더보기' }));

    // 책장 이름 바꾸기 버튼이 표시되는지 확인하고 클릭한다.
    const renameButton = screen.getByRole('button', { name: '책장 이름 바꾸기' });
    expect(renameButton).toBeVisible();
    await user.click(renameButton);

    // 책장 이름 변경 팝업과 입력란이 표시되는지 확인한다.
    const renameTitle = screen.getByText('책장 이름 바꾸기');
    expect(renameTitle).toBeVisible();
    const renameDialog = renameTitle.closest('.ui-Dialog-Container');
    expect(renameDialog).not.toBeNull();
    const popup = within(renameDialog as HTMLElement);
    const nameInput = popup.getByRole('textbox');
    expect(nameInput).toBeVisible();

    // 기존 책장 이름을 지우고 새 이름을 입력한다.
    await user.clear(nameInput);
    await user.type(nameInput, renamedLibraryName);

    // 저장 버튼을 클릭한다.
    await user.click(popup.getByRole('button', { name: '저장' }));

    // 변경한 책장 이름이 표시되는지 확인한다.
    expect(await screen.findByText(renamedLibraryName)).toBeVisible();
  });

  it('특정 책장의 이름을 변경하면 해당 책장의 이름만 변경된다', async () => {
    const user = userEvent.setup();
    const currentLibraries = structuredClone(librariesBeforeRenameResponse);

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(currentLibraries));
      }),
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId`, ({ params }) => {
        const library = currentLibraries.libraries.find(
          (item) => item.libraryId === Number(params.libraryId),
        );
        return library
          ? HttpResponse.json(structuredClone(library))
          : new HttpResponse(null, { status: 404 });
      }),
      http.patch(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId`, async ({ params, request }) => {
        const library = currentLibraries.libraries.find(
          (item) => item.libraryId === Number(params.libraryId),
        );
        if (!library) return new HttpResponse(null, { status: 404 });
        const { name } = (await request.json()) as { name: string };
        library.name = name;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    // 서재 페이지에 진입한다.
    const { unmount } = renderProvider(
      <Routes>
        <Route path="/libraries" element={<LibrariesPage />} />
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      { auth: defaultLoggedAuthContextValue, initialEntries: ['/libraries'] },
    );

    // 이름을 변경할 책장과 다른 책장이 표시되는지 확인한다.
    const targetLibrary = await screen.findByRole('link', {
      name: libraryResponse.name,
      exact: true,
    });
    expect(targetLibrary).toBeVisible();
    expect(
      screen.getByRole('link', { name: otherLibraryResponse.name, exact: true }),
    ).toBeVisible();

    // 이름을 변경할 책장을 선택한다.
    await user.click(targetLibrary);
    expect(await screen.findByText(libraryResponse.name, { exact: true })).toBeVisible();

    // 책장 더보기 메뉴에서 책장 이름 바꾸기를 선택한다.
    await user.click(screen.getByRole('button', { name: '책장 더보기', exact: true }));
    await user.click(screen.getByRole('button', { name: '책장 이름 바꾸기', exact: true }));

    // 기존 이름을 지우고 새로운 책장 이름을 입력한다.
    const nameInput = screen.getByPlaceholderText('예: 다시 펼치고 싶은 책');
    await user.clear(nameInput);
    await user.type(nameInput, renamedLibraryName);

    // 저장 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '저장', exact: true }));

    // 상세 화면에 변경한 책장 이름이 표시되고 이름 변경 팝업이 닫히는지 확인한다.
    expect(await screen.findByText(renamedLibraryName, { exact: true })).toBeVisible();
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: '저장', exact: true })).not.toBeInTheDocument();
    });

    // 서재 메뉴를 선택해 책장 목록으로 돌아간다.
    const librariesLink = screen.getByRole('link', { name: '내 서재', exact: true });
    expect(librariesLink).toHaveAttribute('href', '/libraries');
    // 일반 링크의 전체 페이지 이동은 jsdom에서 지원하지 않아 목적지 화면을 새로 연다.
    unmount();
    renderProvider(<LibrariesPage />, {
      auth: defaultLoggedAuthContextValue,
      initialEntries: ['/libraries'],
    });

    // 선택한 책장에만 새 이름이 표시되고 다른 책장의 이름은 유지되는지 확인한다.
    const renamedLibrary = await screen.findByRole('link', {
      name: renamedLibraryName,
      exact: true,
    });
    expect(renamedLibrary).toBeVisible();
    expect(renamedLibrary).toHaveAttribute('href', `/libraries/${libraryResponse.libraryId}`);
    const otherLibrary = screen.getByRole('link', { name: otherLibraryResponse.name, exact: true });
    expect(otherLibrary).toBeVisible();
    expect(otherLibrary).toHaveAttribute('href', `/libraries/${otherLibraryResponse.libraryId}`);
    expect(
      screen.queryByRole('link', { name: libraryResponse.name, exact: true }),
    ).not.toBeInTheDocument();
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

    // 책장 페이지에 진입한다.
    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 책장 제목 옆의 더보기 버튼을 클릭한다.
    const libraryName = await screen.findByText(libraryResponse.name);
    const libraryTitle = libraryName.closest('.ui-Title');
    expect(libraryTitle).not.toBeNull();
    await user.click(within(libraryTitle as HTMLElement).getByRole('button', { name: '책장 더보기' }));

    // 이미지 저장 메뉴를 클릭한다.
    await user.click(screen.getByRole('button', { name: '이미지 저장' }));

    // 책장을 이미지로 저장하는 팝업이 표시되는지 확인한다.
    const imageDialogTitle = screen.getByText('책장을 이미지로');
    expect(imageDialogTitle).toBeVisible();
    const imageDialog = imageDialogTitle.closest('.ui-Dialog-Container');
    expect(imageDialog).not.toBeNull();

    // 팝업의 이미지 저장 버튼을 클릭한다.
    await user.click(
      within(imageDialog as HTMLElement).getByRole('button', { name: '이미지 저장' }),
    );

    // 책장 이미지가 PNG 파일로 다운로드되는지 확인한다.
    await waitFor(() => {
      expect(downloads).toEqual([
        { href: downloadedLibraryImageDataUrl, filename: expect.stringMatching(/\.png$/) },
      ]);
    });
    expect(toDataURL).toHaveBeenCalledWith('image/png');
    expect(html2canvas).toHaveBeenCalledTimes(1);

    // 저장한 이미지에 현재 책장의 책등이 모두 포함되는지 확인한다.
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

  it('내 서재에서 탭의 책을 선택하여 책장에 추가할 수 있다', async () => {
    const user = userEvent.setup();
    let isBookAdded = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(
          structuredClone(isBookAdded ? libraryAfterAddResponse : libraryResponse),
        );
      }),
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
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

    // 책장 페이지에 진입한다.
    const { container } = renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 현재 책장 이름이 표시되는지 확인한다.
    expect(await screen.findByText(libraryResponse.name)).toBeVisible();

    // 추가할 책이 현재 책장에 없는지 확인한다.
    const libraryContent = container.querySelector<HTMLElement>('.frame-Main');
    if (!libraryContent) throw new Error('책장 본문을 찾을 수 없습니다.');
    const library = within(libraryContent);
    expect(library.queryByText(searchedBook.title)).not.toBeInTheDocument();

    // 책 담기 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '책 담기' }));

    // 책 담기 팝업에서 내 서재에서 탭을 선택한다.
    const dialogTitle = screen.getByText('책장에 책 담기');
    const dialog = dialogTitle.closest<HTMLElement>('.ui-Dialog-Container');
    if (!dialog) throw new Error('책 담기 팝업을 찾을 수 없습니다.');
    const popup = within(dialog);
    await user.click(popup.getByText('내 서재에서'));

    // 내 서재의 책이 목록에 표시되는지 확인한다.
    const libraryBook = await popup.findByText(searchedBook.title);
    expect(libraryBook).toBeVisible();

    // 내 서재 목록에서 추가할 책을 선택한다.
    const bookItem = libraryBook.closest<HTMLElement>('.ui-List-Item');
    if (!bookItem) throw new Error('내 서재 책의 목록 항목을 찾을 수 없습니다.');
    const checkbox = within(bookItem).getByRole('checkbox');
    await user.click(checkbox);

    // 선택한 책의 체크박스가 체크되는지 확인한다.
    expect(checkbox).toBeChecked();

    // 담기 버튼을 클릭해 선택한 책을 책장에 추가한다.
    await user.click(popup.getByRole('button', { name: '담기' }));

    // 책장에 추가한 책이 표시되는지 확인한다.
    expect(await library.findByText(searchedBook.title)).toBeVisible();
  });

  it('책 검색 탭에서 검색한 책을 선택하여 책장에 추가할 수 있다', async () => {
    const user = userEvent.setup();
    let isBookAdded = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(myLibraryResponse));
      }),
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
      // 책장 페이지에 진입한다.
      const { container } = renderProvider(
        <Routes>
          <Route path="/libraries/:libraryId" element={<LibraryPage />} />
        </Routes>,
        {
          auth: defaultLoggedAuthContextValue,
          initialEntries: [`/libraries/${libraryResponse.libraryId}`],
        },
      );

      // 현재 책장 이름이 표시되는지 확인한다.
      expect(await screen.findByText(libraryResponse.name)).toBeVisible();

      // 추가할 책이 현재 책장에 없는지 확인한다.
      const libraryContent = container.querySelector<HTMLElement>('.frame-Main');
      if (!libraryContent) throw new Error('책장 본문을 찾을 수 없습니다.');
      const library = within(libraryContent);
      expect(library.queryByText(searchedBook.title)).not.toBeInTheDocument();

      // 책 담기 버튼을 클릭한다.
      await user.click(screen.getByRole('button', { name: '책 담기' }));

      // 책 담기 팝업에서 책 검색 탭을 선택한다.
      const dialogTitle = screen.getByText('책장에 책 담기');
      const dialog = dialogTitle.closest<HTMLElement>('.ui-Dialog-Container');
      if (!dialog) throw new Error('책 담기 팝업을 찾을 수 없습니다.');
      const popup = within(dialog);
      await user.click(popup.getByText('책 검색'));

      // 추가할 책 제목을 검색한다.
      await user.type(
        popup.getByPlaceholderText('책 제목이나 작가를 검색하세요'),
        searchedBook.title,
      );

      // 검색한 책이 결과에 표시되는지 확인한다.
      const searchResult = await popup.findByText(searchedBook.title);
      expect(searchResult).toBeVisible();

      // 검색 결과에서 추가할 책을 선택한다.
      const bookItem = searchResult.closest<HTMLElement>('.ui-List-Item');
      if (!bookItem) throw new Error('검색한 책의 목록 항목을 찾을 수 없습니다.');
      const checkbox = within(bookItem).getByRole('checkbox');
      await user.click(checkbox);

      // 선택한 책의 체크박스가 체크되는지 확인한다.
      expect(checkbox).toBeChecked();

      // 담기 버튼을 클릭해 선택한 책을 책장에 추가한다.
      await user.click(popup.getByRole('button', { name: '담기' }));

      // 책장에 추가한 책이 표시되는지 확인한다.
      expect(await library.findByText(searchedBook.title)).toBeVisible();
    } finally {
      if (scrollToDescriptor) {
        Object.defineProperty(HTMLElement.prototype, 'scrollTo', scrollToDescriptor);
      } else {
        Reflect.deleteProperty(HTMLElement.prototype, 'scrollTo');
      }
    }
  });

  it('책장을 삭제하면 서재 페이지로 이동한다', async () => {
    const user = userEvent.setup();

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${deletableLibraryResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(deletableLibraryResponse));
      }),
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(librariesAfterDeleteResponse));
      }),
      http.delete(
        `${ENV.APP_API_URL}/api/v1/libraries/${deletableLibraryResponse.libraryId}`,
        () => {
          return new HttpResponse(null, { status: 204 });
        },
      ),
    );

    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
        <Route path="/libraries" element={<LibrariesPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${deletableLibraryResponse.libraryId}`],
      },
    );

    // 더보기 버튼을 클릭한다.
    const libraryName = await screen.findByText(deletableLibraryResponse.name);
    const libraryTitle = libraryName.closest<HTMLElement>('.ui-Title');
    if (!libraryTitle) throw new Error('책장 제목을 찾을 수 없습니다.');
    await user.click(within(libraryTitle).getByRole('button', { name: '책장 더보기' }));

    // 관리 팝업에서 책장 삭제를 선택한다.
    await user.click(screen.getByRole('button', { name: '책장 삭제' }));

    // 삭제 팝업에서 삭제 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '책장 삭제' }));

    // 서재 페이지로 이동했는지 확인한다.
    expect(await screen.findByText('나의 책장')).toBeVisible();
  });

  it('편집 모드에서 책의 삭제 버튼을 클릭하면 해당 책이 목록에서 제거된다', async () => {
    const user = userEvent.setup();
    let isBookDeleted = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(
          structuredClone(isBookDeleted ? libraryAfterBookDeleteResponse : libraryResponse),
        );
      }),
      http.delete(
        `${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}/books/${bookToDelete.bookId}`,
        () => {
          isBookDeleted = true;
          return new HttpResponse(null, { status: 204 });
        },
      ),
    );

    // 책장 페이지에 진입한다.
    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 삭제할 책과 남겨둘 책이 목록에 표시되는지 확인한다.
    const bookTitle = await screen.findByText(bookToDelete.title);
    expect(bookTitle).toBeVisible();
    expect(await screen.findByText(bookToKeep.title)).toBeVisible();

    // 편집 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '편집' }));

    // 삭제할 책의 목록 항목에 삭제 버튼이 표시되는지 확인한다.
    const bookItem = bookTitle.closest<HTMLElement>('.ui-List-Item');
    if (!bookItem) throw new Error('삭제할 책의 목록 항목을 찾을 수 없습니다.');
    const deleteButton = within(bookItem).getByRole('button', { name: /삭제/ });
    expect(deleteButton).toBeVisible();

    // 해당 책의 삭제 버튼을 클릭한다.
    await user.click(deleteButton);

    // 삭제한 책은 목록에서 사라지고 다른 책은 그대로 표시되는지 확인한다.
    await waitFor(() => {
      expect(screen.queryByText(bookToDelete.title)).not.toBeInTheDocument();
    });
    expect(screen.getByText(bookToKeep.title)).toBeVisible();
  });

  it('책 삭제를 선택하면 편집 모드로 전환되고, 삭제 버튼을 누르면 해당 책이 목록에서 제거된다', async () => {
    const user = userEvent.setup();
    let isBookDeleted = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(
          structuredClone(isBookDeleted ? libraryAfterBookDeleteResponse : libraryResponse),
        );
      }),
      http.delete(
        `${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}/books/${bookToDelete.bookId}`,
        () => {
          isBookDeleted = true;
          return new HttpResponse(null, { status: 204 });
        },
      ),
    );

    // 책장 페이지에 진입한다.
    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 삭제할 책과 남겨둘 책이 목록에 표시되는지 확인한다.
    const bookTitle = await screen.findByText(bookToDelete.title);
    expect(bookTitle).toBeVisible();
    expect(await screen.findByText(bookToKeep.title)).toBeVisible();

    // 책장 제목 옆의 더보기 버튼을 클릭한다.
    const libraryName = screen.getByText(libraryResponse.name);
    const libraryTitle = libraryName.closest<HTMLElement>('.ui-Title');
    if (!libraryTitle) throw new Error('책장 제목을 찾을 수 없습니다.');
    await user.click(within(libraryTitle).getByRole('button', { name: '책장 더보기' }));

    // 관리 팝업에 책 삭제 메뉴가 표시되는지 확인한다.
    const deleteBooksButton = screen.getByRole('button', { name: '책 삭제', exact: true });
    expect(deleteBooksButton).toBeVisible();

    // 관리 팝업에서 책 삭제 메뉴를 선택한다.
    await user.click(deleteBooksButton);

    // 관리 팝업이 닫히고 편집 모드의 완료 버튼이 표시되는지 확인한다.
    expect(screen.queryByRole('button', { name: '책 삭제', exact: true })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '완료', exact: true })).toBeVisible();

    // 삭제할 책의 목록 항목에 삭제 버튼이 표시되는지 확인한다.
    const bookItem = bookTitle.closest<HTMLElement>('.ui-List-Item');
    if (!bookItem) throw new Error('삭제할 책의 목록 항목을 찾을 수 없습니다.');
    const deleteButton = within(bookItem).getByRole('button', { name: /삭제/ });
    expect(deleteButton).toBeVisible();

    // 해당 책의 삭제 버튼을 클릭한다.
    await user.click(deleteButton);

    // 삭제한 책은 목록에서 사라지고 다른 책은 그대로 표시되는지 확인한다.
    await waitFor(() => {
      expect(screen.queryByText(bookToDelete.title)).not.toBeInTheDocument();
    });
    expect(screen.getByText(bookToKeep.title)).toBeVisible();
  });

  it('기본 책장의 관리 팝업에는 책장 삭제 버튼이 표시되지 않는다', async () => {
    const user = userEvent.setup();

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(libraryResponse));
      }),
    );

    // 기본 책장 페이지에 진입한다.
    renderProvider(
      <Routes>
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      {
        auth: defaultLoggedAuthContextValue,
        initialEntries: [`/libraries/${libraryResponse.libraryId}`],
      },
    );

    // 기본 책장의 더보기 버튼을 클릭한다.
    const libraryName = await screen.findByText(libraryResponse.name);
    const libraryTitle = libraryName.closest<HTMLElement>('.ui-Title');
    if (!libraryTitle) throw new Error('기본 책장의 제목을 찾을 수 없습니다.');
    await user.click(within(libraryTitle).getByRole('button', { name: '책장 더보기' }));

    // 관리 팝업에 책장 이름 바꾸기 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '책장 이름 바꾸기', exact: true })).toBeVisible();

    // 기본 책장의 관리 팝업에는 책장 삭제 버튼이 표시되지 않는지 확인한다.
    expect(
      screen.queryByRole('button', { name: '책장 삭제', exact: true }),
    ).not.toBeInTheDocument();
  });
});
