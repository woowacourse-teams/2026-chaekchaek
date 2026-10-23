import { screen, waitFor } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';

import { describe, expect, it } from 'vitest';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibraryPage } from '@/pages/LibraryPage';

import { libraryResponse } from './LibraryPage.fixtures';

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
      http.get(`${ENV.APP_API_URL}/api/v1/libraries/${libraryResponse.libraryId}`, () => {
        return HttpResponse.json(structuredClone(libraryResponse));
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

      for (const book of libraryResponse.items) {
        const spineImage = images.find((image) => image.getAttribute('src') === book.spineImageUrl);

        expect(spineImage, `${book.title} 책등 이미지`).toBeDefined();
        expect(spineImage).toBeVisible();
      }
    });
  });
});
