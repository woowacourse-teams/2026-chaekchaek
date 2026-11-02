import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';

import { describe, expect, it, vi } from 'vitest';
import html2canvas from 'html2canvas';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibrariesPage } from '@/pages/LibrariesPage';
import { LibraryPage } from '@/pages/LibraryPage';

import {
  libraryToEditResponse,
  libraryToShare,
  libraryImageDataUrl,
  createdLibraryResponse,
  librariesAfterCreateResponse,
  librariesResponse,
  librariesAfterDeleteResponse,
  libraryToDelete,
  libraryToKeep,
  libraryToRename,
  renamedLibraryName,
} from './LibrariesPage.fixtures';

vi.mock('html2canvas', () => ({ default: vi.fn() }));

describe('LibrariesPage', () => {
  it('기본 렌더링이 된다', () => {
    renderProvider(<LibrariesPage />);

    expect(screen.getByText('나의 책장')).toBeInTheDocument();
  });

  it('내 책장에 진입하면 등록된 책장 목록이 표시된다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(librariesResponse));
      }),
    );

    const { container } = renderProvider(<LibrariesPage />, {
      auth: defaultLoggedAuthContextValue,
    });

    // API가 반환한 모든 책장 이름과 각 책의 책등 이미지가 표시된다.
    for (const library of librariesResponse.libraries) {
      expect(await screen.findByText(library.name)).toBeVisible();

      for (const book of library.items) {
        // 책등 컴포넌트의 이미지는 alt가 비어 있어 img 역할로 조회되지 않는다.
        const images = Array.from(container.querySelectorAll('img'));
        const spineImage = images.find((image) => image.getAttribute('src') === book.spineImageUrl);

        expect(spineImage, `${library.name}의 ${book.title} 책등 이미지`).toBeDefined();
        expect(spineImage).toBeVisible();
      }
    }
  });

  it('책장 추가 버튼을 클릭하면 팝업이 열리고, 이름을 입력해 책장을 추가할 수 있다', async () => {
    const user = userEvent.setup();
    let isLibraryCreated = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        if (isLibraryCreated) {
          return HttpResponse.json(structuredClone(librariesAfterCreateResponse));
        }

        return HttpResponse.json(structuredClone(librariesResponse));
      }),
      http.post(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        isLibraryCreated = true;

        return HttpResponse.json(structuredClone(createdLibraryResponse), { status: 201 });
      }),
    );

    renderProvider(<LibrariesPage />, { auth: defaultLoggedAuthContextValue });

    for (const library of librariesResponse.libraries) {
      expect(await screen.findByRole('link', { name: library.name })).toBeVisible();
    }
    expect(screen.queryByRole('link', { name: createdLibraryResponse.name })).not.toBeInTheDocument();

    // 1. 책장 추가 버튼 클릭
    await user.click(screen.getByRole('button', { name: '책장 추가' }));

    // 2. 책장 추가 팝업 표시 확인
    const popup = screen.getByRole('dialog', { name: '새 책장 만들기' });
    expect(popup).toBeVisible();
    expect(within(popup).getByText('책장 이름')).toBeVisible();

    // 3. 책장 이름 입력 후 추가 버튼 클릭
    await user.type(within(popup).getByRole('textbox'), createdLibraryResponse.name);
    await user.click(within(popup).getByRole('button', { name: '만들기' }));

    // 4. 추가한 책장이 목록에 표시되는지 확인한다.
    expect(await screen.findByRole('link', { name: createdLibraryResponse.name })).toBeVisible();
  });

  it('특정 책장의 이름을 변경하면 해당 책장의 이름만 변경된다', async () => {
    const user = userEvent.setup();
    const currentLibraries = structuredClone(librariesResponse);

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(currentLibraries));
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

    // 여러 책장이 있는 서재 페이지에 진입한다.
    renderProvider(<LibrariesPage />, { auth: defaultLoggedAuthContextValue });

    // 이름을 변경할 책장과 다른 책장이 표시되는지 확인한다.
    const libraryLink = await screen.findByRole('link', {
      name: libraryToRename.name,
      exact: true,
    });
    expect(libraryLink).toBeVisible();
    for (const library of librariesResponse.libraries) {
      expect(screen.getByRole('link', { name: library.name, exact: true })).toBeVisible();
    }

    // 이름을 변경할 책장의 더보기 버튼을 클릭한다.
    const libraryTitle = libraryLink.closest<HTMLElement>('.ui-Title');
    if (!libraryTitle) throw new Error('이름을 변경할 책장의 제목을 찾을 수 없습니다.');
    const [, moreButton] = within(libraryTitle).getAllByRole('button');
    if (!moreButton) throw new Error('책장 더보기 버튼을 찾을 수 없습니다.');
    await user.click(moreButton);

    // 관리 팝업에서 책장 이름 바꾸기를 선택한다.
    await user.click(screen.getByRole('button', { name: '책장 이름 바꾸기', exact: true }));

    // 기존 책장 이름을 지우고 새로운 이름을 입력한다.
    const nameInput = screen.getByPlaceholderText('예: 다시 펼치고 싶은 책');
    expect(nameInput).toHaveValue(libraryToRename.name);
    await user.clear(nameInput);
    await user.type(nameInput, renamedLibraryName);

    // 저장 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '저장', exact: true }));

    // 선택한 책장의 이름이 새 이름으로 바뀌고 기존 이름은 사라지는지 확인한다.
    const renamedLibrary = await screen.findByRole('link', {
      name: renamedLibraryName,
      exact: true,
    });
    expect(renamedLibrary).toBeVisible();
    expect(renamedLibrary).toHaveAttribute('href', `/libraries/${libraryToRename.libraryId}`);
    expect(
      screen.queryByRole('link', { name: libraryToRename.name, exact: true }),
    ).not.toBeInTheDocument();

    // 다른 책장들의 이름과 연결된 책장이 그대로 유지되는지 확인한다.
    for (const library of librariesResponse.libraries.filter(
      (item) => item.libraryId !== libraryToRename.libraryId,
    )) {
      const unchangedLibrary = screen.getByRole('link', { name: library.name, exact: true });
      expect(unchangedLibrary).toBeVisible();
      expect(unchangedLibrary).toHaveAttribute('href', `/libraries/${library.libraryId}`);
    }
  });

  it('특정 책장의 이미지를 저장하면 해당 책장의 책등 이미지가 다운로드된다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(librariesResponse));
      }),
    );
    const canvas = document.createElement('canvas');
    const toDataURL = vi.spyOn(canvas, 'toDataURL').mockReturnValue(libraryImageDataUrl);
    vi.mocked(html2canvas).mockResolvedValue(canvas);
    const downloadImage = vi.fn<(href: string, filename: string, connected: boolean) => void>();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      downloadImage(this.href, this.download, this.isConnected);
    });

    // 여러 책장이 있는 서재 페이지에 진입한다.
    renderProvider(<LibrariesPage />, { auth: defaultLoggedAuthContextValue });
    const libraryLink = await screen.findByRole('link', { name: libraryToShare.name, exact: true });
    for (const library of librariesResponse.libraries) {
      expect(screen.getByRole('link', { name: library.name, exact: true })).toBeVisible();
    }

    // 이미지를 저장할 책장의 더보기 버튼을 클릭한다.
    const libraryTitle = libraryLink.closest<HTMLElement>('.ui-Title');
    if (!libraryTitle) throw new Error('이미지를 저장할 책장의 제목을 찾을 수 없습니다.');
    const [, moreButton] = within(libraryTitle).getAllByRole('button');
    if (!moreButton) throw new Error('책장 더보기 버튼을 찾을 수 없습니다.');
    await user.click(moreButton);

    // 관리 팝업에서 이미지 저장 메뉴를 클릭한다.
    await user.click(screen.getByRole('button', { name: '이미지 저장', exact: true }));

    // 이미지 저장 팝업에 선택한 책장의 책등 이미지만 표시되는지 확인한다.
    const popup = screen.getByText('책장을 이미지로').closest<HTMLElement>('.ui-Dialog');
    if (!popup) throw new Error('책장 이미지 저장 팝업을 찾을 수 없습니다.');
    expect(popup).toBeVisible();
    const preview = within(popup).getByText(libraryToShare.name).closest<HTMLElement>('.ui-DisplayCard');
    if (!preview) throw new Error('저장할 책장 이미지 미리보기를 찾을 수 없습니다.');
    expect(preview).toBeVisible();
    const expectedSpines = libraryToShare.items.map((book) => book.spineImageUrl);
    expect(Array.from(preview.querySelectorAll('img'), (image) => image.getAttribute('src'))).toEqual(
      expectedSpines,
    );

    // 이미지 저장 버튼을 클릭한다.
    await user.click(within(popup).getByRole('button', { name: '이미지 저장', exact: true }));

    // 현재 책장의 책등 이미지를 담은 PNG 파일 다운로드가 실행되는지 확인한다.
    await waitFor(() => {
      expect(downloadImage).toHaveBeenCalledExactlyOnceWith(
        libraryImageDataUrl,
        `책췍_${defaultLoggedAuthContextValue.user.nickname}_${libraryToShare.name}.png`,
        true,
      );
    });
    expect(html2canvas).toHaveBeenCalledTimes(1);
    const capturedLibrary = vi.mocked(html2canvas).mock.calls[0][0];
    expect(capturedLibrary).toBe(preview);
    expect(capturedLibrary).toHaveTextContent(libraryToShare.name);
    expect(
      Array.from(capturedLibrary.querySelectorAll('img'), (image) => image.getAttribute('src')),
    ).toEqual(expectedSpines);
    expect(toDataURL).toHaveBeenCalledExactlyOnceWith('image/png');
  });

  it('책장을 삭제하면 해당 책장이 목록에서 제거된다', async () => {
    const user = userEvent.setup();
    let isLibraryDeleted = false;

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(
          structuredClone(isLibraryDeleted ? librariesAfterDeleteResponse : librariesResponse),
        );
      }),
      http.delete(`${ENV.APP_API_URL}/api/v1/libraries/${libraryToDelete.libraryId}`, () => {
        isLibraryDeleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    // 서재 페이지에 진입한다.
    renderProvider(<LibrariesPage />, { auth: defaultLoggedAuthContextValue });

    // 삭제할 책장과 남겨둘 책장이 목록에 표시되는지 확인한다.
    const libraryLink = await screen.findByRole('link', { name: libraryToDelete.name });
    expect(libraryLink).toBeVisible();
    expect(await screen.findByRole('link', { name: libraryToKeep.name })).toBeVisible();

    // 삭제할 책장의 더보기 버튼을 클릭한다.
    const libraryTitle = libraryLink.closest<HTMLElement>('.ui-Title');
    if (!libraryTitle) throw new Error('삭제할 책장의 제목을 찾을 수 없습니다.');
    const [, moreButton] = within(libraryTitle).getAllByRole('button');
    if (!moreButton) throw new Error('책장 더보기 버튼을 찾을 수 없습니다.');
    await user.click(moreButton);

    // 관리 팝업에서 책장 삭제를 선택한다.
    await user.click(screen.getByRole('button', { name: '책장 삭제', exact: true }));

    // 삭제 팝업에서 책장 삭제 버튼을 클릭한다.
    expect(screen.getByText('책장을 삭제할까요?')).toBeVisible();
    await user.click(screen.getByRole('button', { name: '책장 삭제', exact: true }));

    // 선택한 책장이 목록에서 제거되었는지 확인한다.
    await waitFor(() => {
      expect(screen.queryByRole('link', { name: libraryToDelete.name })).not.toBeInTheDocument();
    });

    // 다른 책장은 목록에 그대로 표시되는지 확인한다.
    expect(screen.getByRole('link', { name: libraryToKeep.name })).toBeVisible();
  });

  it('특정 책장의 책 삭제를 선택하면 해당 책장 페이지로 이동하고 편집 모드로 전환된다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(librariesResponse));
      }),
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryToEditResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(libraryToEditResponse));
      }),
    );

    // 여러 책장이 표시된 서재 페이지에 진입한다.
    renderProvider(
      <Routes>
        <Route path="/libraries" element={<LibrariesPage />} />
        <Route path="/libraries/:libraryId" element={<LibraryPage />} />
      </Routes>,
      { auth: defaultLoggedAuthContextValue, initialEntries: ['/libraries'] },
    );
    const libraryLink = await screen.findByRole('link', {
      name: libraryToEditResponse.name,
      exact: true,
    });
    for (const library of librariesResponse.libraries) {
      expect(screen.getByRole('link', { name: library.name, exact: true })).toBeVisible();
    }

    // 책을 삭제할 책장의 더보기 버튼을 클릭한다.
    const libraryTitle = libraryLink.closest<HTMLElement>('.ui-Title');
    if (!libraryTitle) throw new Error('책을 삭제할 책장의 제목을 찾을 수 없습니다.');
    const [, moreButton] = within(libraryTitle).getAllByRole('button');
    if (!moreButton) throw new Error('책장 더보기 버튼을 찾을 수 없습니다.');
    await user.click(moreButton);

    // 관리 팝업에 책 삭제 메뉴가 표시되는지 확인한다.
    const deleteBooksButton = screen.getByRole('button', { name: '책 삭제', exact: true });
    expect(deleteBooksButton).toBeVisible();

    // 책 삭제 메뉴를 선택한다.
    await user.click(deleteBooksButton);

    // 선택한 책장 페이지로 이동해 해당 책장의 이름과 책 목록이 표시되는지 확인한다.
    expect(await screen.findByText('이 책장에 담긴 책')).toBeVisible();
    expect(await screen.findByText(libraryToEditResponse.name, { exact: true })).toBeVisible();
    expect(
      screen.queryByRole('link', { name: libraryToEditResponse.name, exact: true }),
    ).not.toBeInTheDocument();

    // 편집 모드의 완료 버튼과 각 책의 삭제 버튼이 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '완료', exact: true })).toBeVisible();
    expect(screen.queryByRole('button', { name: '편집', exact: true })).not.toBeInTheDocument();
    for (const book of libraryToEditResponse.items) {
      const bookItem = screen.getByText(book.title).closest<HTMLElement>('.ui-List-Item');
      if (!bookItem) throw new Error('책 목록 항목을 찾을 수 없습니다.');
      expect(within(bookItem).getByRole('button', { name: /삭제/ })).toBeVisible();
    }
  });
});
