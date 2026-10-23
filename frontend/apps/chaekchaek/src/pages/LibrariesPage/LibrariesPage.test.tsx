import { act, fireEvent, screen } from '@testing-library/react';

import { describe, expect, it, vi } from 'vitest';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibrariesPage } from '@/pages/LibrariesPage';

import { registeredLibraries } from './LibrariesPage.fixtures';

describe('LibrariesPage', () => {
  it('기본 렌더링이 된다', () => {
    renderProvider(<LibrariesPage />);

    expect(screen.getByText('나의 책장')).toBeInTheDocument();
  });

  it('내 책장에 진입하면 등록된 책장 목록이 표시된다', async () => {
    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => {
        return HttpResponse.json(structuredClone(registeredLibraries));
      }),
    );

    const { container } = renderProvider(<LibrariesPage />, {
      auth: defaultLoggedAuthContextValue,
    });

    for (const library of registeredLibraries.libraries) {
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
});
