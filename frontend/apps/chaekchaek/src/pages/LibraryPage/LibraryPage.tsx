import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ComponentProps } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';

import { DragDropProvider } from '@dnd-kit/react';
import { isSortable } from '@dnd-kit/react/sortable';

type DragEndEvent = Parameters<
  NonNullable<ComponentProps<typeof DragDropProvider>['onDragEnd']>
>[0];

import {
  Button,
  ConstrainedImgBox,
  ContentArea,
  Divider,
  Icon,
  IconButton,
  ImageGradientBackground,
  ImgBox,
  List,
  Notice,
  ResponsiveText,
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
import { useExecute } from '@/services/core/useExecute';
import { patchMembersMeAnonymity } from '@/services/apis/membersMeAnonymity/repository';
import { deleteLibrariesLibraryIdBooksBookId } from '@/services/apis/librariesLibraryIdBooksBookId/repository';
import { patchLibrariesLibraryIdBooksOrder } from '@/services/apis/librariesLibraryIdBooksOrder/repository';

import { ManageLibraryDialog } from '@/shared/ManageLibrary';
import type { ManageLibraryDialogType } from '@/shared/ManageLibrary';

import { ShareLibraryImageDialog } from '@/shared/ShareLibraryImage';
import { RenameLibraryDialog } from '@/shared/RenameLibrary';
import { DeleteLibraryDialog } from '@/shared/DeleteLibrary';

import { ROUTES } from '@/constants/routes';

import { track } from '@/analytics/track';

import { AddBooksToLibraryDialog } from './dialog/AddBooksToLibraryDialog';
import { UpdateNicknameDialog } from './dialog/UpdateNicknameDialog';

export type LibraryPageDialogType =
  | ManageLibraryDialogType
  | 'ManageLibraryDialog'
  | 'AddBooksToLibraryDialog'
  | 'UpdateNicknameDialog';

const booksHeight = [
  ...[286, 304, 318, 312, 296, 282, 288, 306, 320, 298], // 10
  ...[286, 304, 318, 312, 296, 282, 288, 306, 320, 298], // 10
  ...[286, 304, 318, 312, 296, 282, 288, 306, 320, 298], // 10
];
const canDeleteLibrary = ({ isDefault }: { isDefault: boolean }): boolean => {
  return !isDefault;
};

import { Sortable } from './components/Sortable/Sortable';

const move = <T,>(array: T[], from: number, to: number): T[] => {
  const result = [...array];

  const [item] = result.splice(from, 1);

  if (item === undefined) return array;

  result.splice(to, 0, item);

  return result;
};

export const LibraryPage = () => {
  const { user, updateAccount } = useAuthContext();

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

  const [orderedBookIds, setOrderedBookIds] = useState<number[] | null>(null);

  useEffect(() => {
    if (!libraryItemsData) return;
    setOrderedBookIds(libraryItemsData.items.map((item) => item.bookId));
  }, [libraryItemsData]);

  const books = useMemo(() => {
    if (!libraryItemsData) return [];

    if (!orderedBookIds) return libraryItemsData.items;

    const bookMap = new Map(libraryItemsData.items.map((book) => [book.bookId, book]));

    return orderedBookIds.map((id) => bookMap.get(id)).filter((book) => book !== undefined);
  }, [libraryItemsData, orderedBookIds]);

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

  const { status: anonymityStatus, mutate: updateAnonymity } = useExecute({
    executeFn: patchMembersMeAnonymity,
  });

  const handleToggleAnonymous = async () => {
    if (!user) return;

    if (user.displayAnonymous) {
      if (!user.nickname) return handleOpenDialog('UpdateNicknameDialog');

      const userWithAnonymityDisabled = await updateAnonymity({ displayAnonymous: false });
      if (!userWithAnonymityDisabled) return;

      updateAccount({
        ...user,
        ...userWithAnonymityDisabled,
      });

      return;
    }

    const updatedUser = await updateAnonymity({ displayAnonymous: true });
    if (!updatedUser) return;

    updateAccount({
      ...user,
      ...updatedUser,
    });
  };

  const location = useLocation();
  const state = location.state as { startEditing: boolean };

  const handleClickManageLibrary = () => {
    handleOpenDialog('ManageLibraryDialog');
    track('library_management_open');
  };

  const handleCLickShareImage = () => {
    handleOpenDialog('ShareLibraryImageDialog');
    track('library_save_image_open');
  };

  const handleClickAddBook = () => {
    handleOpenDialog('AddBooksToLibraryDialog');
    track('library_add_open', {
      source: 'library',
    });
  };

  const [isEditing, setIsEditing] = useState(state?.startEditing || false);
  const handleClickStartEdit = () => {
    setIsEditing(true);
    track('library_start_edit');
  };
  const handleClickEndEdit = () => {
    setIsEditing(false);
    track('library_end_edit');
  };

  const { mutate: deleteBookMutate } = useExecute({
    executeFn: deleteLibrariesLibraryIdBooksBookId,
  });

  const handleClickDeleteBook = async (bookId: number) => {
    await deleteBookMutate({ libraryId: Number(libraryId), bookId });

    track('library_remove');

    await refetchGetLibrary();
  };

  const navigate = useNavigate();
  const handleGoToLibraries = async () => {
    navigate(ROUTES.LIBRARIES);
  };

  const { mutate: patchBookOrderMutate } = useExecute({
    executeFn: patchLibrariesLibraryIdBooksOrder,
  });

  const handleDragEnd = async (event: DragEndEvent) => {
    if (!libraryItemsData) return;

    const { source } = event.operation;
    if (!source || !isSortable(source)) return;

    const { initialIndex, index } = source;
    if (initialIndex === index) return;

    const reorderedBooks = move(libraryItemsData?.items, initialIndex, index);

    const reorderBooksId = reorderedBooks.map((item) => item.bookId);

    await patchBookOrderMutate({
      libraryId: Number(libraryId),
      bookIds: reorderBooksId,
    });

    track('library_reorder_book');

    setOrderedBookIds(reorderBooksId);
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
        return (
          <AddBooksToLibraryDialog
            libraryId={Number(libraryId)}
            onBookAdded={async () => {
              await refetchGetLibrary();
            }}
            onClose={handleCloseDialog}
          />
        );
      case 'ManageLibraryDialog':
        return (
          libraryItemsData && (
            <ManageLibraryDialog
              canDeleteLibrary={canDeleteLibrary(libraryItemsData)}
              libraryName={libraryItemsData.name}
              onStartEdit={handleClickStartEdit}
              onOpenDialog={handleOpenDialog}
              onClose={handleCloseDialog}
            />
          )
        );
      case 'ShareLibraryImageDialog':
        return (
          libraryItemsData && (
            <ShareLibraryImageDialog
              libraryName={libraryItemsData.name}
              imgs={libraryItemsData?.items.map((item) => {
                return {
                  title: item.title,
                  spine: item.spineImageUrl,
                  cover: item.coverImageUrl,
                };
              })}

              onClose={handleCloseDialog}
            />
          )
        );

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
        return (
          libraryItemsData && (
            <DeleteLibraryDialog
              libraryId={Number(libraryId)}
              libraryName={libraryItemsData.name}
              onLibraryDeleted={handleGoToLibraries}
              onClose={handleCloseDialog}
            />
          )
        );
      case 'UpdateNicknameDialog':
        return <UpdateNicknameDialog onClose={handleCloseDialog} />;

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
                <>
                  {user !== null && (
                    <>
                      {user?.displayAnonymous && (
                        <Button
                          leading={<Icon.CheckboxOnIcon color="secondary" />}
                          variant="ghost"
                          size="small"
                          disabled={anonymityStatus.status === 'loading'}
                          onClick={handleToggleAnonymous}
                        >
                          익명으로 감상 공개
                        </Button>
                      )}
                      {!user?.displayAnonymous && (
                        <Button
                          leading={<Icon.CheckboxOffIcon color="secondary" />}
                          variant="ghost"
                          size="small"
                          disabled={anonymityStatus.status === 'loading'}
                          onClick={handleToggleAnonymous}
                        >
                          익명으로 감상 공개
                        </Button>
                      )}
                    </>
                  )}

                  <IconButton
                    aria-label="책장 더보기"
                    variant="secondary"
                    onClick={handleClickManageLibrary}
                  >
                    <Icon.EllipsisIcon />
                  </IconButton>
                </>
              }
            >
              {libraryItemsData?.name}
            </Title>
            {libraryItemsData !== null && !!libraryItemsData?.items.length && (
              <Showcase>
                <DragDropProvider onDragEnd={handleDragEnd}>
                  {books.map((item, index) => {
                    return (
                      <Sortable key={item.bookId} id={item.bookId} index={index}>
                        <ConstrainedImgBox
                          {...(!!item.spineImageUrl && {
                            img: item.spineImageUrl,
                          })}
                          fallback={
                            <ImageGradientBackground
                              img={item.coverImageUrl}
                              width="40px"
                              height={`${booksHeight[index]}px`}
                            >
                              <ResponsiveText sx={{ mt: 4 }} direction="vertical">
                                {item.title}
                              </ResponsiveText>
                            </ImageGradientBackground>
                          }
                          maxWidth="40px"
                          height={`${booksHeight[index]}px`}
                        />
                      </Sortable>
                    );
                  })}
                </DragDropProvider>
              </Showcase>
            )}

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
              <Button variant="primary" sx={{ mr: 4 }} onClick={handleClickAddBook}>
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
                onClick={handleCLickShareImage}
              >
                <Icon.DownloadIcon size="large" />
              </IconButton>
            </Title>

            <Title
              sx={{ mt: 8 }}
              level="detail"
              trailing={
                <>
                  {!isEditing ? (
                    <Button variant="default" onClick={handleClickStartEdit}>
                      편집
                    </Button>
                  ) : (
                    <Button variant="default" onClick={handleClickEndEdit}>
                      완료
                    </Button>
                  )}
                </>
              }
            >
              이 책장에 담긴 책
            </Title>

            <Divider sx={{ mt: 4 }} />

            {!libraryItemsData?.items.length && (
              <Notice height={500}>이 책장에 등록된 책이 없습니다.</Notice>
            )}

            <DragDropProvider onDragEnd={handleDragEnd}>
              <List columns={2}>
                {books.map((item, index) => {
                  return (
                    <Sortable key={item.bookId} id={item.bookId} index={index}>
                      <List.Item>
                        <List.Item.Leading>
                          <Link to={`/books/${item.isbn13}`}>
                            <ImgBox img={item.coverImageUrl} size="small" />
                          </Link>
                        </List.Item.Leading>
                        <List.Item.Content
                          title={<>{item.title}</>}
                          content={item.authors?.join(' · ')}
                        />

                        <List.Item.Trailing>
                          {isEditing ? (
                            <IconButton
                              aria-label="빼기"
                              onClick={() => {
                                handleClickDeleteBook(item.bookId);
                              }}
                            >
                              <Icon.CloseIcon color="error" />
                            </IconButton>
                          ) : (
                            <Sortable.Handle>
                              <IconButton edge="start">
                                <Icon.DragHandleIcon />
                              </IconButton>
                            </Sortable.Handle>
                          )}
                        </List.Item.Trailing>
                      </List.Item>
                    </Sortable>
                  );
                })}
              </List>
            </DragDropProvider>
          </ContentArea>
        </Container>
      </Main>

      {dialogElement}
    </Layout>
  );
};
