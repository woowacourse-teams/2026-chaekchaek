import { render, screen } from '@testing-library/react';

import { describe, expect, it } from 'vitest';

import { App } from '@/App';

describe('App', () => {
  it('기본 렌더링이 된다', () => {
    render(<App />);

    expect(screen.getByText(/책첵/i)).toBeInTheDocument();
  });
});
