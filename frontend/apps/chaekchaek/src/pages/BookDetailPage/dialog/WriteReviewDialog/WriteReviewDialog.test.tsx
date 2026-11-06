import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { WriteReviewDialog } from './WriteReviewDialog';

describe('WriteReviewDialog', () => {
  it('본문을 입력해야 제출할 수 있고 스포일러 선택을 전환할 수 있다', async () => {
    renderProvider(
      <WriteReviewDialog
        bookId={1}
        isbn="9788936433598"
        myRating={undefined}
        onReviewWritten={vi.fn()}
        onClose={vi.fn()}
      />,
      { auth: defaultLoggedAuthContextValue },
    );
    const user = userEvent.setup();
    const submit = screen.getByRole('button', { name: '감상 남기기' });
    expect(submit).toBeDisabled();
    expect(screen.getByText('감상을 입력해 주세요.')).toBeVisible();
    expect(screen.getByText('선택 안 함')).toBeVisible();

    const spoiler = screen.getByRole('radio', { name: '스포일러 포함' });
    await user.click(spoiler);
    expect(spoiler).toBeChecked();
    await user.click(spoiler);
    expect(spoiler).not.toBeChecked();

    const content = screen.getByPlaceholderText('이 책을 읽으며 든 생각을 남겨보세요.');
    await user.type(content, '기억에 남는 책이에요.');
    expect(submit).toBeEnabled();
    expect(screen.queryByText('감상을 입력해 주세요.')).not.toBeInTheDocument();
    await user.clear(content);
    expect(submit).toBeDisabled();
  });

  it('기존 별점이 있으면 미선택 안내 대신 점수와 대응 문구를 유지한다', () => {
    renderProvider(
      <WriteReviewDialog
        bookId={1}
        isbn="9788936433598"
        myRating={4}
        onReviewWritten={vi.fn()}
        onClose={vi.fn()}
      />,
      { auth: defaultLoggedAuthContextValue },
    );
    expect(screen.getByText('4 · 좋았어요')).toBeVisible();
    expect(screen.queryByText('선택 안 함')).not.toBeInTheDocument();
  });
});
