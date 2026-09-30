import { screen } from '@testing-library/react';

import { describe, expect, it } from 'vitest';

import { renderProvider } from '@/test/utils/render';

import { App } from '@/App';

describe('App', () => {
  it('기본 렌더링이 된다', async () => {
    renderProvider(<App />);

    expect(screen.getAllByText(/책책/).length).toBeGreaterThan(0);
  });
});
