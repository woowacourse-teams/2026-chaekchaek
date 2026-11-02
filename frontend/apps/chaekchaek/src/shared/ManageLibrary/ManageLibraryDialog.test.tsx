import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderProvider } from '@/test/utils/render';

import { ManageLibraryDialog } from './ManageLibraryDialog';
import { libraryName } from './ManageLibraryDialog.fixtures';

describe('ManageLibraryDialog', () => {
  it('책장 관리 메뉴를 표시한다', () => {
    // 책장의 관리 팝업을 연다.
    renderProvider(
      <ManageLibraryDialog
        libraryName={libraryName}
        onStartEdit={vi.fn()}
        onOpenDialog={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    // 관리 중인 책장 이름이 표시되는지 확인한다.
    expect(screen.getByText(libraryName)).toBeVisible();

    // 책장 이름 바꾸기 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '책장 이름 바꾸기', exact: true })).toBeVisible();

    // 책 삭제 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '책 삭제', exact: true })).toBeVisible();

    // 이미지 저장 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '이미지 저장', exact: true })).toBeVisible();

    // 책장 삭제 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '책장 삭제', exact: true })).toBeVisible();
  });
});
