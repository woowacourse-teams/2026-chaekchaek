import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';

import { describe, expect, it } from 'vitest';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibraryPage } from '@/pages/LibraryPage';

import { fullLibraryResponse, libraryResponse, renamedLibraryName } from './LibraryPage.fixtures';

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
});
