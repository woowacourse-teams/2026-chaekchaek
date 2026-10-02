import { act, fireEvent, screen } from '@testing-library/react';

import { describe, expect, it, vi } from 'vitest';

import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';

import { renderProvider } from '@/test/utils/render';

import { BookFeedPage } from '@/pages/BookFeedPage/BookFeedPage';

import { feedReviewsPage1, feedReviewsPage2 } from './BookFeedPage.fixtures';

describe('BookFeedPage', () => {
  it('기본 렌더링이 된다', () => {
    renderProvider(<BookFeedPage />);

    expect(screen.getByText('전체 감상 피드')).toBeInTheDocument();
  });

  it('피드에 진입하면 피드 목록을 조회해 화면에 보여준다', async () => {
    const onFeedRequest = vi.fn();

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/feed/reviews`, ({ request }) => {
        const url = new URL(request.url);
        onFeedRequest(url.searchParams.get('page'));

        return HttpResponse.json(feedReviewsPage1);
      }),
    );

    renderProvider(<BookFeedPage />);

    for (const review of feedReviewsPage1.reviews) {
      expect(await screen.findByText(review.bookTitle)).toBeInTheDocument();
      expect(screen.getByText(review.author.displayName)).toBeInTheDocument();
      expect(screen.getByText(review.content)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: `답글 ${review.replyCount}` })).toBeInTheDocument();
    }

    expect(onFeedRequest).toHaveBeenCalledWith('1');
  });

  it('스크롤을 끝까지 내리면 다음 피드 목록을 조회해 화면에 보여준다', async () => {
    const onFeedRequest = vi.fn();
    let scrollTop = 0;

    // jsdom은 레이아웃과 scrollTo를 구현하지 않으므로 스크롤 위치와 이벤트를 재현한다.
    vi.stubGlobal('innerHeight', 800);
    vi.stubGlobal('scrollY', 0);
    vi.stubGlobal('pageYOffset', 0);
    vi.spyOn(document.documentElement, 'scrollHeight', 'get').mockReturnValue(2000);
    vi.spyOn(document.documentElement, 'clientHeight', 'get').mockReturnValue(800);
    vi.spyOn(document.documentElement, 'scrollTop', 'get').mockImplementation(() => scrollTop);
    vi.spyOn(window, 'scrollTo').mockImplementation(
      (options?: ScrollToOptions | number, y?: number) => {
        scrollTop = typeof options === 'number' ? (y ?? 0) : (options?.top ?? 0);
        vi.stubGlobal('scrollY', scrollTop);
        vi.stubGlobal('pageYOffset', scrollTop);
        fireEvent.scroll(window);
      },
    );

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/feed/reviews`, ({ request }) => {
        const page = new URL(request.url).searchParams.get('page');
        onFeedRequest(page);

        return HttpResponse.json(page === '2' ? feedReviewsPage2 : feedReviewsPage1);
      }),
    );

    try {
      renderProvider(<BookFeedPage />);

      for (const review of feedReviewsPage1.reviews) {
        expect(await screen.findByText(review.content)).toBeInTheDocument();
      }
      expect(onFeedRequest).toHaveBeenCalledWith('1');
      expect(onFeedRequest).not.toHaveBeenCalledWith('2');
      for (const review of feedReviewsPage2.reviews) {
        expect(screen.queryByText(review.content)).not.toBeInTheDocument();
      }

      act(() => {
        window.scrollTo({ top: document.documentElement.scrollHeight - window.innerHeight });
      });

      for (const review of feedReviewsPage2.reviews) {
        expect(await screen.findByText(review.content)).toBeInTheDocument();
        expect(screen.getByText(review.bookTitle)).toBeInTheDocument();
        expect(screen.getByText(review.author.displayName)).toBeInTheDocument();
        expect(
          screen.getByRole('button', { name: `답글 ${review.replyCount}` }),
        ).toBeInTheDocument();
      }
      expect(onFeedRequest).toHaveBeenCalledWith('2');

      for (const review of feedReviewsPage1.reviews) {
        expect(screen.getByText(review.content)).toBeInTheDocument();
      }
    } finally {
      vi.unstubAllGlobals();
      vi.restoreAllMocks();
    }
  });
});
