import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import {
  Button,
  ConstrainedImgBox,
  ContentArea,
  Divider,
  Icon,
  IconButton,
  ImgBox,
  List,
  Notice,
  Showcase,
  Surface,
  Text,
  Title,
} from '@chaekchaek/design-system';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { getLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';
import { useLoadData } from '@/services/core/useLoadData';

import { AddBooksToLibraryDialog } from './dialog/AddBooksToLibraryDialog';
import { ManageLibraryDialog } from './dialog/ManageLibraryDialog';
import { ShareLibraryImageDialog } from './dialog/ShareLibraryImageDialog';
import { RenameLibraryDialog } from './dialog/RenameLibraryDialog';
import { DeleteLibraryDialog } from './dialog/DeleteLibraryDialog';

export type LibraryPageDialogType =
  | 'AddBooksToLibraryDialog'
  | 'ManageLibraryDialog'
  | 'ShareLibraryImageDialog'
  | 'RenameLibraryDialog'
  | 'DeleteLibraryDialog';

const booksHeight = [
  ...[286, 304, 318, 312, 296, 282, 288, 306, 320, 298], // 10
  ...[286, 304, 318, 312, 296, 282, 288, 306, 320, 298], // 10
  ...[286, 304, 318, 312, 296, 282, 288, 306, 320, 298], // 10
];

export const LibraryPage = () => {
  const { user } = useAuthContext();

  const { libraryId } = useParams<{ libraryId: string }>();

  const getLibrariesLibraryIdLoadData = useCallback(async () => {
    return await getLibrariesLibraryId({
      libraryId: Number(libraryId),
    });
  }, [libraryId]);

  const {
    status: { data: libraryItemsData },
    refetch: refetchGetLibrary,
  } = useLoadData({
    queryFn: getLibrariesLibraryIdLoadData,
  });

  const [isCopied, setIsCopied] = useState(false);

  useEffect(() => {
    const TIME_OUT = 1500;
    if (isCopied)
      setTimeout(() => {
        setIsCopied(false);
      }, TIME_OUT);
  }, [isCopied]);

  const handleCopyLink = async () => {
    const memberLibraryLink = `${window.location.origin}/members/${user?.memberId}/libraries/${libraryId}`;
    await navigator.clipboard.writeText(memberLibraryLink);

    setIsCopied(true);
  };

  const [dialog, setDialog] = useState<LibraryPageDialogType | null>(null);
  const handleOpenDialog = (dialog: LibraryPageDialogType) => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: LibraryPageDialogType | null) => {
    switch (dialog) {
      case 'AddBooksToLibraryDialog':
        return <AddBooksToLibraryDialog onClose={handleCloseDialog} />;
      case 'ManageLibraryDialog':
        return <ManageLibraryDialog onClose={handleCloseDialog} onOpenDialog={handleOpenDialog} />;
      case 'ShareLibraryImageDialog':
        return <ShareLibraryImageDialog onClose={handleCloseDialog} />;

      case 'RenameLibraryDialog':
        return (
          libraryItemsData && (
            <RenameLibraryDialog
              libraryId={Number(libraryId)}
              name={libraryItemsData?.name}
              onLibraryRenamed={async () => {
                await refetchGetLibrary();
              }}
              onClose={handleCloseDialog}
            />
          )
        );
      case 'DeleteLibraryDialog':
        return <DeleteLibraryDialog onClose={handleCloseDialog} />;

      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  return (
    <Layout>
      <Header />
      <Main>
        <Container>
          <ContentArea spacing="large">
            <Title
              level="main"
              sx={{ mb: 14 }}
              description={`${libraryItemsData?.name} 책장 ${libraryItemsData?.totalCount}권`}
              trailing={
                <IconButton
                  variant="secondary"
                  onClick={() => {
                    handleOpenDialog('ManageLibraryDialog');
                  }}
                >
                  <Icon.EllipsisIcon />
                </IconButton>
              }
            >
              {libraryItemsData?.name}
            </Title>
            <Showcase>
              {libraryItemsData?.items.map((item, index) => {
                return (
                  <ConstrainedImgBox
                    img={item.spineImageUrl}
                    maxWidth="80px"
                    maxHeight={`${booksHeight[index]}px`}
                  />
                );
              })}
            </Showcase>

            {libraryItemsData !== null && !libraryItemsData?.items.length && (
              <Surface
                sx={{ mt: 14 }}
                variant="dashed"
                verticalAlign="center"
                style={{ height: '320px' }}
              >
                <Text color="muted">어떤 책으로 채워볼까?</Text>
              </Surface>
            )}

            <Title sx={{ mt: 8 }} level="detail">
              <Button
                variant="primary"
                sx={{ mr: 4 }}
                onClick={() => {
                  handleOpenDialog('AddBooksToLibraryDialog');
                }}
              >
                <Icon.PlusIcon color="inverse" />책 담기
              </Button>
              {/* <IconButton variant="ghost" size="large" sx={{ mr: 2 }} onClick={handleCopyLink}>
                {isCopied ? (
                  <Icon.CheckAnimatedIcon size="large" />
                ) : (
                  <Icon.UploadIcon size="large" />
                )}
              </IconButton> */}
              <IconButton
                variant="ghost"
                size="large"
                sx={{ mr: 2 }}
                onClick={() => {
                  handleOpenDialog('ShareLibraryImageDialog');
                }}
              >
                <Icon.DownloadIcon size="large" />
              </IconButton>
            </Title>

            <Title
              sx={{ mt: 8 }}
              level="detail"
              trailing={
                <>
                  <Button variant="default">편집</Button>
                </>
              }
            >
              이 책장에 담긴 책
            </Title>

            <Divider sx={{ mt: 4 }} />

            {!libraryItemsData?.items.length && (
              <Notice height={500}>이 책장에 등록된 책이 없습니다.</Notice>
            )}

            <List columns={2}>
              {libraryItemsData?.items.map((item) => {
                return (
                  <List.Item key={item.bookId}>
                    <List.Item.Leading>
                      <IconButton edge="start">
                        <Icon.DragHandleIcon />
                      </IconButton>
                      <Link to={`/books/${item.isbn13}`}>
                        <ImgBox img={item.coverImageUrl} size="small" />
                      </Link>
                    </List.Item.Leading>
                    <List.Item.Content
                      title={<>{item.title}</>}
                      content={item.authors?.join(' · ')}
                    />
                    <List.Item.Trailing></List.Item.Trailing>
                  </List.Item>
                );
              })}
            </List>
          </ContentArea>
        </Container>
      </Main>

      {dialogElement}
    </Layout>
  );
};
