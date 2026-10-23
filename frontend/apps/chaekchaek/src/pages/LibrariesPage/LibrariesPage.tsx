import { useCallback, useState } from 'react';
import { generatePath, Link } from 'react-router-dom';

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

import { ROUTES } from '@/constants/routes';

import { track } from '@/analytics/track';

import { RegisterLibraryDialog } from './dialog/RegisterLibraryDialog';

export const LibrariesPage = () => {
  const { user } = useAuthContext();
  const getLibrariesLoadData = useCallback(async () => {
    if (!user?.memberId) return;
    return await getLibraries({ memberId: user?.memberId });
  }, [user?.memberId]);

  const {
    status: { data: librariesData },
  } = useLoadData({
    queryFn: getLibrariesLoadData,
  });

  const handleGoToLibrary = () => {
    track('navigate', {
      destination: 'library',
      source: 'libraries',
    });
  };

  const [dialog, setDialog] = useState<'RegisterLibraryDialog' | null>(null);
  const handleOpenDialog = (dialog: 'RegisterLibraryDialog') => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: 'RegisterLibraryDialog' | null) => {
    switch (dialog) {
      case 'RegisterLibraryDialog':
        return <RegisterLibraryDialog onClose={handleCloseDialog} />;

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

            <Partition>
              {librariesData?.libraries.map((library, index) => {
                const odd = index % 2 == 0;
                return (
                  <Partition.Item
                    key={library.libraryId}
                    sx={odd ? { mr: 4 } : { ml: 4 }}
                    style={{ width: '50%' }}
                  >
                    <div>
                      <Title
                        level="content"
                        sx={{ mt: 6, mb: 5 }}
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
                            <IconButton>
                              <Icon.EllipsisIcon />
                            </IconButton>
                          </>
                        }
                      >
                        <IconButton edge="start">
                          <Icon.DragHandleIcon />
                        </IconButton>

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
                  </Partition.Item>
                );
              })}
            </Partition>
          </ContentArea>
        </Container>
      </Main>
      {dialogElement}
    </Layout>
  );
};
