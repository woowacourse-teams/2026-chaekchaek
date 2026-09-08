import { screen } from '@testing-library/react';

import { describe, it, expect } from 'vitest';

import { renderProvider } from '@/test/utils/render';

import { BooksPage } from './BooksPage';

describe('BooksPage', () => {
  it('기본 렌더링이 된다', () => {
    renderProvider(<BooksPage />);

    expect(screen.getByText('책 찾기')).toBeInTheDocument();
  });
});
