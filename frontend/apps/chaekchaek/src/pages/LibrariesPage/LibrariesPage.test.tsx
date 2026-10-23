import { act, fireEvent, screen } from '@testing-library/react';

import { describe, expect, it, vi } from 'vitest';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { renderProvider } from '@/test/utils/render';

import { LibrariesPage } from '@/pages/LibrariesPage';

describe('LibrariesPage', () => {
  it('기본 렌더링이 된다', () => {
    renderProvider(<LibrariesPage />);

    expect(screen.getByText('나의 책장')).toBeInTheDocument();
  });
});
