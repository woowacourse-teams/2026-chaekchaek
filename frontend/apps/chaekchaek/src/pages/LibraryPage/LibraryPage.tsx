import { useState } from 'react';

import {
  Button,
  ConstrainedImgBox,
  ContentArea,
  Divider,
  Icon,
  IconButton,
  ImgBox,
  List,
  Showcase,
  Surface,
  Text,
  Title,
} from '@chaekchaek/design-system';

import ConstrainedImgBoxDummyImg1 from '../../../../../packages/design-system/src/components/ConstrainedImgBox/imgs/dummy1.jpg';
import ImgBoxDummyImg1 from '../../../../../packages/design-system/src/components/ImgBox/imgs/dummy.png';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';
import { Link } from 'react-router-dom';

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

const booksHeight = [286, 304, 318, 312, 296, 282, 288, 306, 320, 298];

export const LibraryPage = () => {
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
        return <RenameLibraryDialog onClose={handleCloseDialog} />;
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
              description={`내 책장 14권`}
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
              책장A
            </Title>
            <Showcase>
              {Array.from({ length: 10 }).map((_, index) => {
                return (
                  <ConstrainedImgBox
                    img={ConstrainedImgBoxDummyImg1}
                    maxWidth="80px"
                    maxHeight={`${booksHeight[index]}px`}
                  />
                );
              })}
            </Showcase>

            <Surface
              sx={{ mt: 14 }}
              variant="dashed"
              verticalAlign="center"
              style={{ height: '320px' }}
            >
              <Text color="muted">어떤 책으로 채워볼까?</Text>
            </Surface>

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
              <IconButton variant="ghost" size="large" sx={{ mr: 2 }}>
                <Icon.UploadIcon size="large" />
              </IconButton>
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

            <List columns={2}>
              {Array.from({ length: 10 }).map(() => {
                const item = {
                  bookId: 1,
                  isbn13: '',
                  coverImageUrl: ImgBoxDummyImg1,
                  title: 'Title',
                  authors: ['author1', 'author2'],
                };

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
                      content={item.authors.join(' · ')}
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
