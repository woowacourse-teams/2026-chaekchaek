import { screen } from '@testing-library/react';
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
});
