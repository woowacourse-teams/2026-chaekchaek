import { useCallback, useEffect } from 'react';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

import { Title, ContentArea } from '@chaekchaek/design-system';

import { useInfiniteLoadData } from '@/services/core/useInfiniteLoadData';
import { getFeedReviews } from '@/services/apis/feedReviews/repository';

import { BookFeed } from './components/BookFeed';

export const BookFeedPage = () => {
  const getFeedReviewsLoadData = useCallback(async ({ pageParam }: { pageParam: number }) => {
    return await getFeedReviews({ page: pageParam });
  }, []);

  const {
    refetch,
    status: { data: reviewsDataAll },
    fetchNextPage,
  } = useInfiniteLoadData({
    queryFn: getFeedReviewsLoadData,
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      return lastPage?.nextPage || 1;
    },
  });

  const feedReviews = (reviewsDataAll?.pages ?? []).flatMap((page) => page.reviews);

  useEffect(() => {
    const handleScroll = async () => {
      const scrollTop = document.documentElement.scrollTop;
      const scrollHeight = document.documentElement.scrollHeight;
      const clientHeight = document.documentElement.clientHeight;

      if (clientHeight >= scrollHeight - scrollTop) {
        if (reviewsDataAll?.pages?.[reviewsDataAll?.pages.length - 1]?.nextPage === null) {
          return;
        }
        await fetchNextPage();
      }
    };
    window.addEventListener('scroll', handleScroll);

    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [reviewsDataAll]);

  return (
    <Layout>
      <Header />
      <Main>
        <Container>
          <ContentArea spacing="large">
            <Title level="main" description="독자들이 남긴 따끈따끈한 문장들" sx={{ mt: 4, mb: 8 }}>
              전체 감상 피드
            </Title>

            {feedReviews.map((review) => {
              return <BookFeed key={review.reviewId} review={review} onFeedRefresh={refetch} />;
            })}
          </ContentArea>
        </Container>
      </Main>
    </Layout>
  );
};
