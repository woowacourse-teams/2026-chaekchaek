import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, it, expect, vi } from 'vitest';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';
import { renderProvider } from '@/test/utils/render';

import { RenameLibraryDialog } from './RenameLibraryDialog';
import { libraryToRename, renamedLibraryName } from './RenameLibraryDialog.fixtures';

describe('RenameLibraryDialog', () => {
  it('새로운 이름을 입력하고 변경 버튼을 클릭하면 책장 이름이 변경된다', async () => {
    const user = userEvent.setup();
    const renameLibrary = vi.fn();
    const onLibraryRenamed = vi.fn(async () => {});
    const onClose = vi.fn();

    server.use(
      http.patch(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId`, async ({ params, request }) => {
        renameLibrary(Number(params.libraryId), await request.json());
        return new HttpResponse(null, { status: 204 });
      }),
    );

    // 책장 이름 변경 팝업을 연다.
    renderProvider(
      <RenameLibraryDialog
        libraryId={libraryToRename.libraryId}
        name={libraryToRename.name}
        onLibraryRenamed={onLibraryRenamed}
        onClose={onClose}
      />,
    );

    // 현재 책장 이름이 표시되는지 확인한다.
    const nameInput = screen.getByRole('textbox');
    expect(nameInput).toHaveValue(libraryToRename.name);

    // 기존 이름을 지우고 새로운 책장 이름을 입력한다.
    await user.clear(nameInput);
    await user.type(nameInput, renamedLibraryName);
    expect(nameInput).toHaveValue(renamedLibraryName);

    // 저장 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '저장', exact: true }));

    // 새 이름으로 변경 요청이 전달되고 변경 완료 후 팝업이 닫히는지 확인한다.
    await waitFor(() => {
      expect(renameLibrary).toHaveBeenCalledExactlyOnceWith(libraryToRename.libraryId, {
        name: renamedLibraryName,
      });
      expect(onLibraryRenamed).toHaveBeenCalledExactlyOnceWith();
      expect(onClose).toHaveBeenCalledExactlyOnceWith();
    });
  });
});
