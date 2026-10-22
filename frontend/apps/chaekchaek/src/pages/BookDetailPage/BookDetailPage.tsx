import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

// import DummyLargeImgBox from '../../components/ImgBox/imgs/dummy-large.png';
import { Split } from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { getBooksIsbn } from '@/services/apis/booksIsbn/repository';
import { postLibrary } from '@/services/apis/library/repository';
import { patchLibraryBookId } from '@/services/apis/libraryBookId/repository';
import { getBooksBookIdReviews } from '@/services/apis/booksBookIdReviews/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { useInfiniteLoadData } from '@/services/core/useInfiniteLoadData';
import { useExecute } from '@/services/core/useExecute';

import { LoginDialog } from '@/pages/LoginPage/dialog/LoginDialog';

import { BookOverview } from './components/BookOverview';
import { BookInfo } from './components/BookInfo';
import { BookReviews } from './components/BookReviews';

export const BookDetailPage = () => {
  const { isbn = '' } = useParams<{ isbn: string }>();

  const getBooksIsbnLoadData = useCallback(async () => {
    return await getBooksIsbn({ isbn });
  }, [isbn]);

  const {
    refetch: refetchGetBooksIsbnLoadData,
    status: { data },
  } = useLoadData({
    queryFn: getBooksIsbnLoadData,
  });

  const { isAuthenticated, guest } = useAuthContext();

  const [openLoginDialog, setOpenLoginDialog] = useState(false);
  const handleOpenLoginDialog = () => {
    if (!isAuthenticated) setOpenLoginDialog(true);
  };
  const handleCloseLoginDialog = () => {
    setOpenLoginDialog(false);
  };

  const { mutate: mutatePatchLibraryBookId } = useExecute({
    executeFn: patchLibraryBookId,
    onSuccess: refetchGetBooksIsbnLoadData,
  });
  const { mutate: mutatePostLibrary } = useExecute({
    executeFn: postLibrary,
    onSuccess: refetchGetBooksIsbnLoadData,
  });
  const handleRegisterLibrary = async (status: string) => {
    if (!isAuthenticated) return handleOpenLoginDialog();
    if (!data?.myRecord) {
      await mutatePostLibrary({ isbn13: isbn, status });

      track('library_add', {
        source: 'detail_info',
        status:
          status === 'WANT_TO_READ'
            ? 'want_to_read'
            : status === 'READING'
              ? 'reading'
              : 'finished',
      });

      return;
    }
    await mutatePatchLibraryBookId({ bookId: data?.bookId, status });
  };

  const [reviewsRequestParams, setReviewsRequestParams] = useState<{
    page: number;
    feed: 'ALL' | 'MINE';
    sort: 'LATEST' | 'OLDEST' | 'POPULAR';
  }>({
    page: 1,
    feed: 'ALL',
    sort: 'LATEST',
  });
  const handleChangeReviewRequestParams = ({
    name,
    value,
  }: {
    name: 'page' | 'feed' | 'sort';
    value: any;
  }) => {
    setReviewsRequestParams((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const getBooksBookIdReviewsLoadData = useCallback(
    async ({ pageParam }: { pageParam: number }) => {
      if (!data?.bookId) return;
      return await getBooksBookIdReviews(
        {
          page: pageParam,
          feed: reviewsRequestParams.feed,
          sort: reviewsRequestParams.sort,
          bookId: data?.bookId,
        },
        guest && guest?.guestToken
          ? {
              guestToken: guest.guestToken,
            }
          : undefined,
      );
    },
    [
      data?.bookId,
      guest?.guestToken,
      isAuthenticated,
      reviewsRequestParams.feed,
      reviewsRequestParams.sort,
    ],
  );

  const {
    status: { data: reviewsDataAll },
    fetchNextPage,
  } = useInfiniteLoadData({
    queryFn: getBooksBookIdReviewsLoadData,
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      return lastPage?.nextPage || 1;
    },
  });

  const reviewsData = (reviewsDataAll?.pages ?? []).flatMap((page) => page?.items).filter(Boolean);
  const totalCount = reviewsDataAll?.pages?.[0]?.totalCount || 0;

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
          <Split>
            <Split.Side>
              <BookInfo
                readingStatus={data?.myRecord?.status}
                category={data?.category}
                publishedDate={data?.publishedDate}
                isbn13={data?.isbn13}
                authors={data?.authors}
                translators={data?.translators}
                purchaseUrl={data?.purchaseUrl}
                onReadingStatusChange={handleRegisterLibrary}
              />
            </Split.Side>
            <Split.Content>
              <BookOverview
                myRating={data?.myRecord?.myRating}
                category={data?.category}
                publishedDate={data?.publishedDate}
                title={data?.title}
                authors={data?.authors}
                publisher={data?.publisher}
                description={data?.description}
                averageRating={data?.averageRating}
                reviewCount={data?.reviewCount}
                replyCount={data?.replyCount}
                coverImageUrl={data?.coverImageUrl}
              />

              <BookReviews
                isbn={isbn}
                {...(data?.bookId && { bookId: data?.bookId })}
                sort={reviewsRequestParams.sort}
                feed={reviewsRequestParams.feed}
                count={totalCount}
                reviews={reviewsData}
                onSortChange={(sort) => {
                  handleChangeReviewRequestParams({ name: 'sort', value: sort });
                }}
                onFeedChange={(feed) => {
                  handleChangeReviewRequestParams({ name: 'feed', value: feed });
                }}
                onReviewsRefresh={refetchGetBooksIsbnLoadData}
              />
            </Split.Content>
          </Split>
        </Container>

        {openLoginDialog && <LoginDialog onClose={handleCloseLoginDialog} />}
      </Main>
    </Layout>
  );
};
