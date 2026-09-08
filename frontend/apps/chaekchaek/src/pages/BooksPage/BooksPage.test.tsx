import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { describe, it, expect } from 'vitest';

import { renderProvider } from '@/test/utils/render';

import { BooksPage } from './BooksPage';

describe('BooksPage', () => {
  it('기본 렌더링이 된다', () => {
    renderProvider(<BooksPage />);

    expect(screen.getByText('책 찾기')).toBeInTheDocument();
  });

  it('검색어를 입력하면 검색 결과를 보여준다', async () => {
    renderProvider(<BooksPage />);

    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: '책 검색' }), '마션');

    expect(await screen.findByText('마션')).toBeInTheDocument();
    expect(await screen.findByText('앤디 위어')).toBeInTheDocument();
  });
});
