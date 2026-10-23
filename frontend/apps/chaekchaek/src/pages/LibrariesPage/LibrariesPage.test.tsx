import { act, fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { describe, expect, it, vi } from 'vitest';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibrariesPage } from '@/pages/LibrariesPage';

import { createdLibraryResponse, librariesResponse } from './LibrariesPage.fixtures';

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
    const createLibraryRequest = vi.fn();
    const response = structuredClone(librariesResponse);

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () => HttpResponse.json(response)),
      http.post(`${ENV.APP_API_URL}/api/v1/libraries`, async ({ request }) => {
        createLibraryRequest(await request.json());
        response.libraries.push(structuredClone(createdLibraryResponse));

        return HttpResponse.json(structuredClone(createdLibraryResponse), { status: 201 });
      }),
    );

    renderProvider(<LibrariesPage />, { auth: defaultLoggedAuthContextValue });

    // 1. 책장 추가 버튼 클릭
    await user.click(screen.getByRole('button', { name: '책장 추가' }));

    // 2. 책장 추가 팝업 표시 확인
    const popup = screen.getByRole('dialog', { name: '새 책장 만들기' });
    expect(popup).toBeVisible();
    expect(within(popup).getByText('책장 이름')).toBeVisible();

    // 3. 책장 이름 입력 후 추가 버튼 클릭
    await user.type(within(popup).getByRole('textbox'), createdLibraryResponse.name);
    await user.click(within(popup).getByRole('button', { name: '만들기' }));

    // 4. 추가한 책장이 화면에 표시되는지 확인
    expect(await screen.findByRole('link', { name: createdLibraryResponse.name })).toBeVisible();
    expect(createLibraryRequest).toHaveBeenCalledExactlyOnceWith({
      name: createdLibraryResponse.name,
    });
  });
});
