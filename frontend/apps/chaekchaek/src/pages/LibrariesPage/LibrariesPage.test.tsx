import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { describe, expect, it } from 'vitest';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { LibrariesPage } from '@/pages/LibrariesPage';

import {
  createdLibraryResponse,
  librariesAfterCreateResponse,
  librariesResponse,
  librariesAfterDeleteResponse,
  libraryToDelete,
  libraryToKeep,
} from './LibrariesPage.fixtures';

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
});
