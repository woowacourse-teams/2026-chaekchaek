import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { Overview } from '@chaekchaek/design-system';
import { ImgBox } from '@chaekchaek/design-system';
// import DummyLargeImgBox from '../../components/ImgBox/imgs/dummy-large.png';
import { Split } from '@chaekchaek/design-system';
import { Title } from '@chaekchaek/design-system';
import { Banner } from '@chaekchaek/design-system';
import { Button } from '@chaekchaek/design-system';
import { ProgressBar } from '@chaekchaek/design-system';
import { SegmentedControl } from '@chaekchaek/design-system';
import { Entry } from '@chaekchaek/design-system';
import { Avatar } from '@chaekchaek/design-system';
// import DummyImgAvatar from '../../components/Avatar/imgs/dummy-avatar.png';
import { Shell } from '@chaekchaek/design-system';
import { Note } from '@chaekchaek/design-system';
import { Surface } from '@chaekchaek/design-system';
import { DataInfo } from '@chaekchaek/design-system';

import { getBooksIsbn } from '@/services/apis/booksIsbn/repository';
import { postLibrary } from '@/services/apis/library/repository';
import { patchLibraryBookId } from '@/services/apis/libraryBookId/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { useExecute } from '@/services/core/useExecute';

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
        <Overview>
          <Overview.Content
            leading={`ARCHIVE / ${data?.category} / ${data?.publishedDate}`}
            title={data?.title}
            content={`${data?.authors} · ${data?.publisher}`}
            description={`${data?.description}

          ${!!data?.myRecord?.myRating ? `별점: ${data?.myRecord?.myRating}` : ''}
          ${`감상: ${data?.reviewCount}`}
          ${`답글: ${data?.replyCount}`}
          `}
          />
          <Overview.Media>
            {data?.coverImageUrl && <ImgBox img={data?.coverImageUrl} />}
          </Overview.Media>
        </Overview>
        <Split>
          <Split.Side>
            <Title level="main">내 독서 기록</Title>
            <Banner>
              <Banner.Content title="내 별점" content="아직 평가하지 않았어요" />
              <Banner.Trailing>
                <Button
                  size="small"
                  variant="primary"
                  onClick={() => {
                    handleOpenDialog('UpdateRatingDialog');
                  }}
                >
                  별점 주기
                </Button>
              </Banner.Trailing>
            </Banner>
            <SegmentedControl
              shape="normal"
              value={data?.myRecord?.status}
              options={[
                {
                  value: 'WANT_TO_READ',
                  text: '읽고 싶어요',
                },
                {
                  value: 'READING',
                  text: '읽는 중',
                },
                {
                  value: 'FINISHED',
                  text: '다 읽음',
                },
              ]}
              onChange={(value: string) => {
                handleRegisterLibrary(value);
              }}
            />

            <ProgressBar
              value={data?.myRecord?.currentPage || 0}
              max={data?.totalPages || 0}
              title="현재 읽은 범위"
              label={`${data?.myRecord?.currentPage || 0} / ${data?.totalPages || 0}쪽`}
            />

            <Button
              variant="primary"
              block={true}
              onClick={() => {
                handleOpenDialog('UpdateCurrentPageDialog');
              }}
            >
              현재 읽은 쪽수 입력
            </Button>
            <DataInfo heading="책 정보">
              {data?.category && <DataInfo.Item title="장르" content={data?.category} />}
              {data?.publishedDate && <DataInfo.Item title="출간" content={data?.publishedDate} />}
              {data?.isbn13 && <DataInfo.Item title="ISBN" content={data?.isbn13} />}
              {!!data?.authors.length && (
                <DataInfo.Item title="지은이" content={data?.authors.join(' · ')} />
              )}
              {!!data?.translators.length && (
                <DataInfo.Item title="옮김" content={data?.translators.join(' · ')} />
              )}
            </DataInfo>
          </Split.Side>
          <Split.Content>
            <Title
              level="main"
              trailing={
                <>
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
              이 책에 남긴 감상 30
            </Title>
            <Entry>
              <Entry.Main>
                <Entry.Header>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                    <Shell.Trailing>Trailing</Shell.Trailing>
                  </Shell>
                </Entry.Header>
                <Entry.Body>
                  Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                  possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                  reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  <Note>
                    Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                    possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                    reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  </Note>
                </Entry.Body>
                <Entry.Footer>
                  <Button size="small" leading={'♡'}>
                    좋아요 2
                  </Button>
                  <Button size="small" leading={'💬'}>
                    답글 2
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
            <Entry variant="subtle">
              <Entry.Main>
                <Entry.Header>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                    <Shell.Trailing>Trailing</Shell.Trailing>
                  </Shell>
                </Entry.Header>
                <Entry.Body>
                  Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                  possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                  reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  <Note>
                    Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                    possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                    reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  </Note>
                </Entry.Body>
                <Entry.Footer>
                  <Button size="small" leading={'♡'}>
                    좋아요 2
                  </Button>
                  <Button size="small" leading={'💬'}>
                    답글 2
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
          </Split.Content>
        </Split>

        {dialogElement}
      </Main>
    </Layout>
  );
};
