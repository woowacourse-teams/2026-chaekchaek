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
  ConstrainedImgBox,
  ContentArea,
  Icon,
  IconButton,
  Partition,
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

import { ManageLibraryDialog } from '@/shared/ManageLibrary';
import type { ManageLibraryDialogType } from '@/shared/ManageLibrary';

import { ROUTES } from '@/constants/routes';

import { track } from '@/analytics/track';

import { RegisterLibraryDialog } from './dialog/RegisterLibraryDialog';
import { RenameLibraryDialog } from '@/shared/RenameLibrary';
import { DeleteLibraryDialog } from '@/shared/DeleteLibrary';
import { ShareLibraryImageDialog } from '@/shared/ShareLibraryImage';

const canDeleteLibrary = ({ isDefault }: { isDefault: boolean }): boolean => {
  return !isDefault;
};

type LibrariesPageDialogType =
  ManageLibraryDialogType | 'RegisterLibraryDialog' | 'ManageLibraryDialog';

import { Sortable } from './components/Sortable/Sortable';

const move = <T,>(array: T[], from: number, to: number): T[] => {
  const result = [...array];

  const [item] = result.splice(from, 1);

  if (item === undefined) return array;

  result.splice(to, 0, item);

  return result;
};

export const LibrariesPage = () => {
  const { user } = useAuthContext();
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

  const handleDragEnd = (event: DragEndEvent) => {
    if (!librariesData) return;

    const { source } = event.operation;
    if (!source || !isSortable(source)) return;

    const { initialIndex, index } = source;
    if (initialIndex === index) return;

    const reorderedLibraries = move(librariesData?.libraries, initialIndex, index);

    console.log(reorderedLibraries);
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
              imgs={library.items.map((item) => ({ title: item.title, url: item.spineImageUrl }))}
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
              description="책장 순서를 드래그로 변경하고, 책등도 순서대로 재배치할 수 있어요."
              trailing={
                <IconButton
                  variant="secondary"
                  aria-label="책장 추가"
                  onClick={() => {
                    handleOpenDialog('RegisterLibraryDialog');
                  }}
                >
                  <Icon.PlusIcon />
                </IconButton>
              }
            >
              나의 책장
            </Title>

            <DragDropProvider onDragEnd={handleDragEnd}>
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
                          {library.items.map((item) => {
                            return (
                              <ConstrainedImgBox
                                key={item.bookId}
                                img={item.spineImageUrl}
                                height="170px"
                              />
                            );
                          })}
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
