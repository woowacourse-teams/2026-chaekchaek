import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderProvider } from '@/test/utils/render';

import { ManageLibraryDialog } from './ManageLibraryDialog';
import { libraryName, nonDeletableLibrary } from './ManageLibraryDialog.fixtures';

describe('ManageLibraryDialog', () => {
  it('책장 관리 메뉴를 표시한다', () => {
    // 책장의 관리 팝업을 연다.
    renderProvider(
      <ManageLibraryDialog
        libraryName={libraryName}
        canDeleteLibrary={true}
        onStartEdit={vi.fn()}
        onOpenDialog={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    // 관리 중인 책장 이름이 표시되는지 확인한다.
    expect(screen.getByText(libraryName)).toBeVisible();

    // 책장 이름 바꾸기 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '책장 이름 바꾸기' })).toBeVisible();

    // 책 삭제 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '책 삭제' })).toBeVisible();

    // 이미지 저장 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '이미지 저장' })).toBeVisible();

    // 책장 삭제 메뉴가 표시되는지 확인한다.
    expect(screen.getByRole('button', { name: '책장 삭제' })).toBeVisible();
  });

  it('삭제할 수 없는 책장은 책장 삭제 버튼을 표시하지 않는다', () => {
    // 삭제할 수 없는 책장의 관리 팝업을 연다.
    renderProvider(
      <ManageLibraryDialog
        libraryName={nonDeletableLibrary.name}
        canDeleteLibrary={nonDeletableLibrary.canDeleteLibrary}
        onStartEdit={vi.fn()}
        onOpenDialog={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    // 관리 중인 책장 이름이 표시되는지 확인한다.
    expect(screen.getByText(nonDeletableLibrary.name)).toBeVisible();

    // 책장 삭제 버튼이 표시되지 않는지 확인한다.
    expect(
      screen.queryByRole('button', { name: '책장 삭제' }),
    ).not.toBeInTheDocument();
  });
});
