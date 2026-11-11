import { useCallback, useState } from 'react';
import type { ComponentProps } from 'react';
import { generatePath, Link, useNavigate } from 'react-router-dom';

import { DragDropProvider } from '@dnd-kit/react';
import { isSortable } from '@dnd-kit/react/sortable';

type DragEndEvent = Parameters<
  NonNullable<ComponentProps<typeof DragDropProvider>['onDragEnd']>
>[0];

import styles from './LibrariesPage.module.css';

import {
  Button,
  ConstrainedImgBox,
  ContentArea,
  Icon,
  IconButton,
  ImageGradientBackground,
  Partition,
  ResponsiveText,
  Showcase,
  Text,
  Title,
} from '@chaekchaek/design-system';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { getLibraries } from '@/services/apis/libraries/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { patchLibrariesOrder } from '@/services/apis/librariesOrder/repository';
import { patchLibrariesLibraryIdBooksOrder } from '@/services/apis/librariesLibraryIdBooksOrder/repository';
import { patchMembersMeAnonymity } from '@/services/apis/membersMeAnonymity/repository';
import { useExecute } from '@/services/core/useExecute';

import { ManageLibraryDialog } from '@/shared/ManageLibrary';
import type { ManageLibraryDialogType } from '@/shared/ManageLibrary';

import { ROUTES } from '@/constants/routes';

import { track } from '@/analytics/track';

import { RegisterLibraryDialog } from './dialog/RegisterLibraryDialog';
import { UpdateNicknameDialog } from './dialog/UpdateNicknameDialog';
import { RenameLibraryDialog } from '@/shared/RenameLibrary';
import { DeleteLibraryDialog } from '@/shared/DeleteLibrary';
import { ShareLibraryImageDialog } from '@/shared/ShareLibraryImage';

const canDeleteLibrary = ({ isDefault }: { isDefault: boolean }): boolean => {
  return !isDefault;
};

type LibrariesPageDialogType =
  | ManageLibraryDialogType
  | 'RegisterLibraryDialog'
  | 'ManageLibraryDialog'
  | 'UpdateNicknameDialog';

import { Sortable } from './components/Sortable/Sortable';

const move = <T,>(array: T[], from: number, to: number): T[] => {
  const result = [...array];

  const [item] = result.splice(from, 1);

  if (item === undefined) return array;

  result.splice(to, 0, item);

  return result;
};

export const LibrariesPage = () => {
  const { user, updateAccount } = useAuthContext();
  const getLibrariesLoadData = useCallback(async () => {
    if (!user?.memberId) return;
    return await getLibraries({ memberId: user?.memberId });
  }, [user?.memberId]);

  const {
    status: { data: librariesData },
    refetch: refetchGetLibraries,
  } = useLoadData({
    queryFn: getLibrariesLoadData,
  });

  const handleGoToLibrary = () => {
    track('navigate', {
      destination: 'library',
      source: 'libraries',
    });
  };

  const [selectedLibrary, setSelectedLibrary] = useState<{
    libraryName: string;
    libraryId: number;
    isDefault: boolean;
  } | null>(null);

  const handleClickManageLibrary = ({
    libraryId,
    libraryName,
    isDefault,
  }: {
    libraryId: number;
    libraryName: string;
    isDefault: boolean;
  }) => {
    setSelectedLibrary({
      libraryId,
      libraryName,
      isDefault,
    });
    handleOpenDialog('ManageLibraryDialog');
  };

  const navigate = useNavigate();
  const handleClickToGoLibrary = (libraryId: number) => {
    navigate(generatePath(ROUTES.LIBRARY, { libraryId: String(libraryId) }), {
      state: {
        startEditing: true,
      },
    });
  };

  const { mutate: patchOrderMutate } = useExecute({
    executeFn: patchLibrariesOrder,
  });

  const handleDragEndLibrary = async (event: DragEndEvent) => {
    if (!librariesData) return;

    const { source } = event.operation;
    if (!source || !isSortable(source)) return;

    const { initialIndex, index } = source;
    if (initialIndex === index) return;

    const reorderedLibraries = move(librariesData?.libraries, initialIndex, index);

    const reorderLibraryIds = reorderedLibraries.map((library) => library.libraryId);

    await patchOrderMutate({
      libraryIds: reorderLibraryIds,
    });
  };

  const { mutate: patchBookOrderMutate } = useExecute({
    executeFn: patchLibrariesLibraryIdBooksOrder,
  });

  const handleDragEndBook = async (event: DragEndEvent, libraryId: number) => {
    if (!librariesData) return;

    const { source } = event.operation;
    if (!source || !isSortable(source)) return;

    const { initialIndex, index } = source;
    if (initialIndex === index) return;

    const libraryItemsData = librariesData.libraries.find(
      (library) => library.libraryId === libraryId,
    );
    if (!libraryItemsData) return;

    const reorderedBooks = move(libraryItemsData?.items, initialIndex, index);

    const reorderBooksId = reorderedBooks.map((item) => item.bookId);

    await patchBookOrderMutate({
      libraryId: Number(libraryId),
      bookIds: reorderBooksId,
    });
  };

  const [dialog, setDialog] = useState<LibrariesPageDialogType | null>(null);
  const handleOpenDialog = (dialog: LibrariesPageDialogType) => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: LibrariesPageDialogType | null) => {
    switch (dialog) {
      case 'RegisterLibraryDialog':
        return (
          <RegisterLibraryDialog
            onLibraryRegistered={async () => {
              await refetchGetLibraries();
            }}
            onClose={handleCloseDialog}
          />
        );
      case 'ManageLibraryDialog':
        return (
          selectedLibrary && (
            <ManageLibraryDialog
              canDeleteLibrary={canDeleteLibrary(selectedLibrary)}
              libraryName={selectedLibrary.libraryName}
              onStartEdit={() => {
                handleClickToGoLibrary(selectedLibrary.libraryId);
              }}
              onOpenDialog={handleOpenDialog}
              onClose={handleCloseDialog}
            />
          )
        );

      case 'ShareLibraryImageDialog':
        const library = librariesData?.libraries.find(
          (library) => library.libraryId === selectedLibrary?.libraryId,
        );

        return (
          selectedLibrary &&
          library && (
            <ShareLibraryImageDialog
              libraryName={selectedLibrary.libraryName}
              imgs={library.items.map((item) => ({
                title: item.title,
                url: item.spineImageUrl ?? item.coverImageUrl,
              }))}
              onClose={handleCloseDialog}
            />
          )
        );
      case 'RenameLibraryDialog':
        return (
          selectedLibrary && (
            <RenameLibraryDialog
              libraryId={Number(selectedLibrary.libraryId)}
              name={selectedLibrary.libraryName}
              onLibraryRenamed={async () => {
                await refetchGetLibraries();
              }}
              onClose={handleCloseDialog}
            />
          )
        );
      case 'DeleteLibraryDialog':
        return (
          selectedLibrary && (
            <DeleteLibraryDialog
              libraryId={selectedLibrary.libraryId}
              libraryName={selectedLibrary.libraryName}
              onLibraryDeleted={async () => {
                await refetchGetLibraries();
              }}
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

  return (
    <Layout>
      <Header />
      <Main>
        <Container>
          <ContentArea spacing="large">
            <Title
              level="main"
              description="책장과 책을 드래그해 순서를 바꿀 수 있어요."
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
                    variant="secondary"
                    aria-label="책장 추가"
                    onClick={() => {
                      handleOpenDialog('RegisterLibraryDialog');
                    }}
                  >
                    <Icon.PlusIcon />
                  </IconButton>
                </>
              }
            >
              내 책장
            </Title>

            <DragDropProvider onDragEnd={handleDragEndLibrary}>
              <Partition columns={1}>
                {librariesData?.libraries.map((library, index) => {
                  return (
                    <Sortable
                      key={library.libraryId}
                      as={Partition.Item}
                      id={library.libraryId}
                      index={index}
                    >
                      <div>
                        <Title
                          level="content"
                          sx={{
                            mt: 6,
                            mb: 5,
                          }}
                          trailing={
                            <>
                              <Text
                                color="muted"
                                as={Link}
                                to={generatePath(ROUTES.LIBRARY, {
                                  libraryId: String(library.libraryId),
                                })}
                                onClick={handleGoToLibrary}
                              >
                                {library.items.length} 권
                              </Text>
                              <IconButton
                                aria-label="책장 관리"
                                onClick={() => {
                                  handleClickManageLibrary({
                                    libraryId: library.libraryId,
                                    libraryName: library.name,
                                    isDefault: library.isDefault,
                                  });
                                }}
                              >
                                <Icon.EllipsisIcon />
                              </IconButton>
                            </>
                          }
                        >
                          <Sortable.Handle>
                            <IconButton edge="start">
                              <Icon.DragHandleIcon />
                            </IconButton>
                          </Sortable.Handle>

                          <Link
                            to={generatePath(ROUTES.LIBRARY, {
                              libraryId: String(library.libraryId),
                            })}
                            onClick={handleGoToLibrary}
                          >
                            {library.name}
                          </Link>
                        </Title>

                        <Showcase>
                          <DragDropProvider
                            onDragEnd={(event) => {
                              handleDragEndBook(event, library.libraryId);
                            }}
                          >
                            {library.items.map((item, index) => {
                              return (
                                <Sortable key={item.bookId} id={item.bookId} index={index}>
                                  <ConstrainedImgBox
                                    {...(!!item.spineImageUrl && {
                                      img: item.spineImageUrl,
                                    })}
                                    maxWidth="20px"
                                    height="170px"
                                    fallback={
                                      <ImageGradientBackground
                                        img={item.coverImageUrl}
                                        width="20px"
                                        height="170px"
                                      >
                                        <ResponsiveText sx={{ mt: 4 }} direction="vertical">
                                          {item.title}
                                        </ResponsiveText>
                                      </ImageGradientBackground>
                                    }
                                  />
                                </Sortable>
                              );
                            })}
                          </DragDropProvider>
                        </Showcase>
                      </div>
                    </Sortable>
                  );
                })}
              </Partition>
            </DragDropProvider>
          </ContentArea>
        </Container>
      </Main>
      {dialogElement}
    </Layout>
  );
};
