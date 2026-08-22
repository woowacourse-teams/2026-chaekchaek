import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

// import DummyLargeImgBox from '../../components/ImgBox/imgs/dummy-large.png';
import { Split } from '@chaekchaek/design-system';
import { Title } from '@chaekchaek/design-system';
import { Button } from '@chaekchaek/design-system';
import { SegmentedControl } from '@chaekchaek/design-system';
import { Select } from '@chaekchaek/design-system';
import { Entry } from '@chaekchaek/design-system';
import { Avatar } from '@chaekchaek/design-system';
// import DummyImgAvatar from '../../components/Avatar/imgs/dummy-avatar.png';
import { Shell } from '@chaekchaek/design-system';
import { Note } from '@chaekchaek/design-system';
import { Surface } from '@chaekchaek/design-system';

import { getBooksIsbn } from '@/services/apis/booksIsbn/repository';
import { postLibrary } from '@/services/apis/library/repository';
import { patchLibraryBookId } from '@/services/apis/libraryBookId/repository';
import { getBooksBookIdReviews } from '@/services/apis/booksBookIdReviews/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { useExecute } from '@/services/core/useExecute';

import { BookOverview } from './components/BookOverview';
import { BookInfo } from './components/BookInfo';
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

  const getBooksBookIdReviewsLoadData = useCallback(async () => {
    if (!data?.bookId) return;
    return await getBooksBookIdReviews({
      page: 1,
      feed: 'ALL',
      sort: 'LATEST',
      bookId: data?.bookId,
    });
  }, [data?.bookId]);

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
            <Title
              level="main"
              trailing={
                <>
                  <Select
                    value="LATEST"
                    options={[
                      { value: 'PAGE', text: '페이지순' },
                      { value: 'LATEST', text: '최신순' },
                      { value: 'OLDEST', text: '오래된순' },
                      { value: 'POPULAR', text: '인기순' },
                    ]}
                  />
                  <SegmentedControl
                    value="all"
                    options={[
                      {
                        value: 'all',
                        text: '전체 피드',
                      },
                      {
                        value: 'mine',
                        text: '내 피드',
                      },
                    ]}
                  />
                </>
              }
            >
              이 책에 남긴 감상 {reviewsData?.totalCount}
            </Title>
            {reviewsData?.items.map((item) => {
              return (
                <Entry key={item.reviewId} variant={item.deleted ? 'subtle' : 'plain'}>
                  <Entry.Main>
                    <Entry.Header>
                      <Shell>
                        <Shell.Leading>
                          <Avatar img={item.author.profileImageUrl} />
                        </Shell.Leading>
                        <Shell.Content
                          title={item.author.displayName}
                          content={new Date(item.createdAt).toLocaleDateString('ko-KR')}
                        />
                        <Shell.Trailing>Trailing</Shell.Trailing>
                      </Shell>
                    </Entry.Header>
                    <Entry.Body>
                      {item.content}
                      {item.quote && <Note>{item.quote}</Note>}
                    </Entry.Body>
                    <Entry.Footer>
                      <Button size="small" leading={item.likedByMe ? '♥' : '♡'}>
                        좋아요 {item.likeCount}
                      </Button>
                      <Button size="small" leading={'💬'}>
                        답글 {item.replyCount}
                      </Button>
                    </Entry.Footer>
                  </Entry.Main>
                  <Entry.Extension>
                    <Surface>
                      <Shell>
                        <Shell.Leading>
                          <Avatar img={''} size="small" />
                        </Shell.Leading>
                        <Shell.Content title="title" content="content" />
                      </Shell>
                    </Surface>
                    <Surface>
                      <Shell>
                        <Shell.Leading>
                          <Avatar img={''} size="small" />
                        </Shell.Leading>
                        <Shell.Content title="title" content="content" />
                      </Shell>
                    </Surface>
                  </Entry.Extension>
                </Entry>
              );
            })}
          </Split.Content>
        </Split>

        {dialogElement}
      </Main>
    </Layout>
  );
};
