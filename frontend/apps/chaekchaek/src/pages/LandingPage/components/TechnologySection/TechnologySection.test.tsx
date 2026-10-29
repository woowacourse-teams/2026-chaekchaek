import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { TechnologySection } from './TechnologySection';

describe('기술 소개 캐러셀', () => {
  it('선택한 슬라이드만 표시하고 이전·다음으로 순환한다', async () => {
    const user = userEvent.setup();
    render(<TechnologySection />);
    expect(screen.getByRole('tabpanel')).toHaveTextContent('캐시 적중률 100%');
    expect(screen.getAllByRole('tabpanel', { hidden: true })).toHaveLength(3);
    await user.click(screen.getByRole('button', { name: '다음 기술 소개' }));
    expect(screen.getByRole('tabpanel')).toHaveTextContent('전체 감상');
    expect(screen.getByText('캐시 적중률 100%').closest('[role="tabpanel"]')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    await user.click(screen.getByRole('tab', { name: /누구나 시작할 수 있는 경험/ }));
    expect(screen.getByRole('tabpanel')).toHaveTextContent('게스트 토큰');
    await user.click(screen.getByRole('button', { name: '다음 기술 소개' }));
    expect(screen.getByRole('tabpanel')).toHaveTextContent('캐시 적중률 100%');
    await user.click(screen.getByRole('button', { name: '이전 기술 소개' }));
    expect(screen.getByRole('tabpanel')).toHaveTextContent('게스트 토큰');
  });

  it('방향키와 Home·End로 탭 선택 및 포커스를 함께 이동한다', () => {
    render(<TechnologySection />);
    const tabs = screen.getAllByRole('tab');
    fireEvent.keyDown(tabs[0], { key: 'ArrowLeft' });
    expect(tabs[2]).toHaveFocus();
    expect(tabs[2]).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel')).toHaveAccessibleName(/누구나 시작할 수 있는 경험/);
    fireEvent.keyDown(tabs[2], { key: 'Home' });
    expect(tabs[0]).toHaveFocus();
    fireEvent.keyDown(tabs[0], { key: 'ArrowRight' });
    expect(tabs[1]).toHaveFocus();
    fireEvent.keyDown(tabs[1], { key: 'End' });
    expect(tabs[2]).toHaveFocus();
    expect(tabs[0]).toHaveAttribute('tabindex', '-1');
  });
});
