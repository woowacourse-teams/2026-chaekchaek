import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { HeroBookshelf } from './HeroBookshelf';
import { getBookLayout } from './bookshelf';

const mockMotionPreference = (reducedMotion = false) => {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches: reducedMotion && query.includes('prefers-reduced-motion'),
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  );
};

const renderBookshelf = () =>
  render(
    <MemoryRouter>
      <HeroBookshelf />
    </MemoryRouter>,
  );

const advanceTime = (milliseconds: number) => {
  act(() => {
    vi.advanceTimersByTime(milliseconds);
  });
};

const expectSelectedTitle = (title: string) => {
  expect(screen.getByRole('status')).toHaveTextContent(title);
};

const completeStep = (hold: number, duration = 1030) => {
  advanceTime(hold);
  advanceTime(duration);
};

describe('HeroBookshelf', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    mockMotionPreference();
    vi.stubGlobal(
      'ResizeObserver',
      class {
        observe = vi.fn();

        unobserve = vi.fn();

        disconnect = vi.fn();
      },
    );
    vi.stubGlobal(
      'PointerEvent',
      class extends MouseEvent {
        readonly pointerType: string;

        constructor(type: string, init: PointerEventInit = {}) {
          super(type, init);
          this.pointerType = init.pointerType ?? '';
        }
      },
    );
  });

  afterEach(() => {
    cleanup();
    vi.clearAllTimers();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('여러 권을 왼쪽으로 이동한 뒤 더 긴 구간을 오른쪽으로 이동하고 반복한다', () => {
    const { container } = renderBookshelf();
    const initialPeripheralBook = container.querySelector<HTMLElement>('[data-slot="-6"]');
    const initialX = Number.parseFloat(
      initialPeripheralBook?.style.getPropertyValue('--book-x') ?? '',
    );

    expectSelectedTitle('이방인');
    advanceTime(1049);
    expectSelectedTitle('이방인');

    advanceTime(1);
    advanceTime(1029);
    expectSelectedTitle('이방인');
    advanceTime(1);
    expectSelectedTitle('밝은 밤');

    for (const title of ['참을 수 없는 존재의 가벼움', '모순', '채식주의자']) {
      completeStep(90);
      expectSelectedTitle(title);
    }

    expect(container.querySelector('[data-slot="-10"]')).toBe(initialPeripheralBook);
    const leftX = Number.parseFloat(
      initialPeripheralBook?.style.getPropertyValue('--book-x') ?? '',
    );
    expect(leftX - initialX).toBeLessThan(-200);

    advanceTime(1999);
    expectSelectedTitle('채식주의자');
    advanceTime(1);
    advanceTime(1249);
    expectSelectedTitle('채식주의자');
    advanceTime(1);
    expectSelectedTitle('모순');

    for (const title of [
      '참을 수 없는 존재의 가벼움',
      '밝은 밤',
      '이방인',
      '작별하지 않는다',
      '사랑의 기술',
      '물고기는 존재하지 않는다',
      '불편한 편의점',
    ]) {
      completeStep(90, 1250);
      expectSelectedTitle(title);
    }

    completeStep(2000);
    expectSelectedTitle('물고기는 존재하지 않는다');
    for (const title of [
      '사랑의 기술',
      '작별하지 않는다',
      '이방인',
      '밝은 밤',
      '참을 수 없는 존재의 가벼움',
      '모순',
      '채식주의자',
    ]) {
      completeStep(90);
      expectSelectedTitle(title);
    }
  });

  it('자동 책장은 탐색 버튼과 책 선택 버튼 없이 상태를 조용히 표시한다', () => {
    const { container } = renderBookshelf();

    expect(screen.queryByRole('button', { name: '이전 책' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '다음 책' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /펼쳐 보기$/ })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveAttribute('aria-live', 'off');
    expect(
      container.querySelector('[data-slot="0"]')?.closest('[aria-hidden="true"]'),
    ).not.toBeNull();
    expect(screen.getByRole('button', { name: '애니메이션 일시정지' })).toBeEnabled();
  });

  it('책 클릭과 방향키, 터치 스와이프는 자동 재생을 취소하거나 선택을 바꾸지 않는다', () => {
    const { container } = renderBookshelf();
    const shelf = screen.getByRole('region', { name: '책장 둘러보기' });
    const book = container.querySelector('[data-slot="-1"]');
    expect(book).not.toBeNull();
    if (!book) throw new Error('책이 렌더링되지 않았습니다.');

    fireEvent.click(book);
    fireEvent.keyDown(shelf, { key: 'ArrowLeft' });
    fireEvent.keyDown(shelf, { key: 'ArrowRight' });
    fireEvent.pointerDown(book, { pointerType: 'touch', clientX: 200, clientY: 100 });
    fireEvent.pointerUp(book, { pointerType: 'touch', clientX: 100, clientY: 110 });
    expectSelectedTitle('이방인');

    advanceTime(1050);
    advanceTime(1030);
    expectSelectedTitle('밝은 밤');
    completeStep(90);
    expectSelectedTitle('참을 수 없는 존재의 가벼움');
  });

  it('일시정지하면 선택을 유지하고 재생하면 2초 후 다음 전환을 시작한다', () => {
    renderBookshelf();
    fireEvent.click(screen.getByRole('button', { name: '애니메이션 일시정지' }));

    advanceTime(10000);
    expectSelectedTitle('이방인');
    expect(vi.getTimerCount()).toBe(0);

    fireEvent.click(screen.getByRole('button', { name: '애니메이션 재생' }));
    advanceTime(1999);
    expectSelectedTitle('이방인');
    advanceTime(1);
    advanceTime(1030);
    expectSelectedTitle('밝은 밤');
  });

  it('전환 중 일시정지하면 현재 책이 정착한 뒤 다음 전환 없이 멈춘다', () => {
    renderBookshelf();
    advanceTime(1050);
    advanceTime(400);

    fireEvent.click(screen.getByRole('button', { name: '애니메이션 일시정지' }));
    advanceTime(630);
    expectSelectedTitle('밝은 밤');
    advanceTime(10000);
    expectSelectedTitle('밝은 밤');
    expect(vi.getTimerCount()).toBe(0);

    fireEvent.click(screen.getByRole('button', { name: '애니메이션 재생' }));
    advanceTime(1999);
    expectSelectedTitle('밝은 밤');
    advanceTime(1);
    advanceTime(1030);
    expectSelectedTitle('참을 수 없는 존재의 가벼움');
  });

  it('오른쪽으로 이동하는 도중 재생을 멈췄다가 이어도 진행 방향을 유지한다', () => {
    renderBookshelf();
    completeStep(1050);
    for (let step = 0; step < 3; step += 1) completeStep(90);
    completeStep(2000, 1250);
    expectSelectedTitle('모순');

    fireEvent.click(screen.getByRole('button', { name: '애니메이션 일시정지' }));
    advanceTime(10000);
    expectSelectedTitle('모순');

    fireEvent.click(screen.getByRole('button', { name: '애니메이션 재생' }));
    completeStep(2000, 1250);
    expectSelectedTitle('참을 수 없는 존재의 가벼움');
  });

  it('동작 줄이기 설정에서는 첫 책을 유지하고 자동 재생 타이머를 만들지 않는다', () => {
    mockMotionPreference(true);
    renderBookshelf();

    expect(vi.getTimerCount()).toBe(0);
    advanceTime(30000);
    expectSelectedTitle('이방인');
    expect(vi.getTimerCount()).toBe(0);
  });

  it('언마운트하면 예약된 반복 타이머와 진행 중인 전환 타이머를 해제한다', () => {
    const scheduled = renderBookshelf();
    expect(vi.getTimerCount()).toBeGreaterThan(0);
    scheduled.unmount();
    expect(vi.getTimerCount()).toBe(0);

    const transitioning = renderBookshelf();
    advanceTime(1050);
    expect(vi.getTimerCount()).toBeGreaterThan(0);
    transitioning.unmount();
    expect(vi.getTimerCount()).toBe(0);
  });
});

describe('책장 슬롯 배치', () => {
  it('양쪽 가장자리의 책도 긴 좌우 이동에 함께 참여하고 출발 위치로 돌아온다', () => {
    for (const index of [0, 12]) {
      const initial = getBookLayout(index, 6, 1);
      const left = getBookLayout(index, 10, 1);
      const right = getBookLayout(index, 2, -1);

      expect(left.x - initial.x).toBeLessThan(-200);
      expect(right.x - left.x).toBeGreaterThan(400);
      expect(getBookLayout(index, 6, -1)).toEqual(initial);
    }
  });

  it('책의 고유 폭으로 서로 다른 슬롯 간격을 유지한다', () => {
    const positions = [0, 1, 2, 3].map((index) => getBookLayout(index, 6, 1).x);
    const gaps = positions.slice(1).map((position, index) => position - positions[index]!);

    expect(gaps.every((gap) => gap > 0)).toBe(true);
    expect(new Set(gaps.map((gap) => Math.round(gap))).size).toBeGreaterThan(1);
  });
});
