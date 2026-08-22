import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

// import DummyLargeImgBox from '../../components/ImgBox/imgs/dummy-large.png';
import { Split } from '@chaekchaek/design-system';

import { getBooksIsbn } from '@/services/apis/booksIsbn/repository';
import { postLibrary } from '@/services/apis/library/repository';
import { patchLibraryBookId } from '@/services/apis/libraryBookId/repository';
import { getBooksBookIdReviews } from '@/services/apis/booksBookIdReviews/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { useExecute } from '@/services/core/useExecute';

import { BookOverview } from './components/BookOverview';
import { BookInfo } from './components/BookInfo';
import { BookReviews } from './components/BookReviews';
import { UpdateCurrentPageDialog } from './dialog/UpdateCurrentPageDialog';
import { UpdateRatingDialog } from './dialog/UpdateRatingDialog';

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

  const { mutate: mutatePatchLibraryBookId } = useExecute({
    executeFn: patchLibraryBookId,
    onSuccess: refetchGetBooksIsbnLoadData,
  });
  const { mutate: mutatePostLibrary } = useExecute({
    executeFn: postLibrary,
    onSuccess: refetchGetBooksIsbnLoadData,
  });
  const handleRegisterLibrary = async (status: string) => {
    if (!data?.myRecord) return await mutatePostLibrary({ isbn13: isbn, status });
    await mutatePatchLibraryBookId({ bookId: data?.bookId, status });
  };

  const [reviewsRequestParams, setReviewsRequestParams] = useState<{
    page: number;
    feed: 'ALL';
    sort: 'LATEST';
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

  const getBooksBookIdReviewsLoadData = useCallback(async () => {
    if (!data?.bookId) return;
    return await getBooksBookIdReviews({
      page: reviewsRequestParams.page,
      feed: reviewsRequestParams.feed,
      sort: reviewsRequestParams.sort,
      bookId: data?.bookId,
    });
  }, [data?.bookId, reviewsRequestParams]);

  const {
    status: { data: reviewsData },
  } = useLoadData({
    queryFn: getBooksBookIdReviewsLoadData,
  });

  const [dialog, setDialog] = useState<'UpdateCurrentPageDialog' | 'UpdateRatingDialog' | null>(
    null,
  );
  const handleOpenDialog = (dialog: 'UpdateCurrentPageDialog' | 'UpdateRatingDialog') => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: 'UpdateCurrentPageDialog' | 'UpdateRatingDialog' | null) => {
    switch (dialog) {
      case 'UpdateCurrentPageDialog':
        return (
          data?.bookId && (
            <UpdateCurrentPageDialog
              bookId={data?.bookId}
              currentPage={data?.myRecord?.currentPage || 0}
              onCurrentPageUpdated={async () => {
                await refetchGetBooksIsbnLoadData();
              }}
              onClose={handleCloseDialog}
            />
          )
        );
      case 'UpdateRatingDialog':
        return (
          data?.bookId && (
            <UpdateRatingDialog
              isbn13={data.isbn13}
              bookId={data?.bookId}
              title={data.title}
              rating={data.myRecord?.myRating}
              myRatingCount={data?.myRatingCount}
              onRatingUpdated={async () => {
                await refetchGetBooksIsbnLoadData();
              }}
              onClose={handleCloseDialog}
            />
          )
        );
      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  return (
    <Layout>
      <Header />
      <Main>
        <BookOverview
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
        <Split>
          <Split.Side>
            <BookInfo
              readingStatus={data?.myRecord?.status}
              currentPage={data?.myRecord?.currentPage}
              totalPages={data?.totalPages}
              category={data?.category}
              publishedDate={data?.publishedDate}
              isbn13={data?.isbn13}
              authors={data?.authors}
              translators={data?.translators}
              onRatingCreate={() => {
                handleOpenDialog('UpdateRatingDialog');
              }}
              onReadingStatusChange={handleRegisterLibrary}
              onCurrentPageUpdate={() => {
                handleOpenDialog('UpdateCurrentPageDialog');
              }}
            />
          </Split.Side>
          <Split.Content>
            <BookReviews
              sort={reviewsRequestParams.sort}
              feed={reviewsRequestParams.feed}
              count={reviewsData?.totalCount}
              reviews={reviewsData?.items}
              onSortChange={(sort) => {
                handleChangeReviewRequestParams({ name: 'sort', value: sort });
              }}
              onFeedChange={(feed) => {
                handleChangeReviewRequestParams({ name: 'feed', value: feed });
              }}
            />
          </Split.Content>
        </Split>

        {dialogElement}
      </Main>
    </Layout>
  );
};
