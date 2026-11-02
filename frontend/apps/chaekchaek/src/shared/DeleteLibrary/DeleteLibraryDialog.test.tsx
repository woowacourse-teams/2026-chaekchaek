import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';
import { renderProvider } from '@/test/utils/render';

import { DeleteLibraryDialog } from './DeleteLibraryDialog';
import { libraryToDelete } from './DeleteLibraryDialog.fixtures';

describe('DeleteLibraryDialog', () => {
  it('삭제 버튼을 클릭하면 해당 책장이 삭제된다', async () => {
    const user = userEvent.setup();
    const deleteLibrary = vi.fn();
    const onLibraryDeleted = vi.fn(async () => {});
    const onClose = vi.fn();

    server.use(
      http.delete(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId`, ({ params }) => {
        deleteLibrary(Number(params.libraryId));
        return new HttpResponse(null, { status: 204 });
      }),
    );

    // 책장 삭제 팝업을 연다.
    renderProvider(
      <DeleteLibraryDialog
        libraryId={libraryToDelete.libraryId}
        libraryName={libraryToDelete.name}
        onLibraryDeleted={onLibraryDeleted}
        onClose={onClose}
      />,
    );

    // 삭제하려는 책장 이름이 안내에 표시되는지 확인한다.
    expect(screen.getByText(`‘${libraryToDelete.name}’ 책장을 삭제합니다.`)).toBeVisible();

    // 책장 삭제 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '책장 삭제', exact: true }));

    // 해당 책장에 삭제 요청이 전달되고 삭제 완료 후 팝업이 닫히는지 확인한다.
    await waitFor(() => {
      expect(deleteLibrary).toHaveBeenCalledExactlyOnceWith(libraryToDelete.libraryId);
      expect(onLibraryDeleted).toHaveBeenCalledExactlyOnceWith();
      expect(onClose).toHaveBeenCalledExactlyOnceWith();
    });
  });
});
