import { useState } from 'react';

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

import DummyImg1 from '../../../../../packages/design-system/src/components/ConstrainedImgBox/imgs/dummy1.jpg';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

import { RegisterLibraryDialog } from './dialog/RegisterLibraryDialog';

export const LibrariesPage = () => {
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
              {Array.from({ length: 2 }).map((_, index) => {
                const odd = index % 2 == 0;
                return (
                  <Partition.Item sx={odd ? { mr: 4 } : { ml: 4 }} style={{ width: '50%' }}>
                    <div>
                      <Title
                        level="content"
                        sx={{ mt: 6, mb: 5 }}
                        trailing={
                          <>
                            <Text color="muted">28권</Text>
                            <IconButton>
                              <Icon.EllipsisIcon />
                            </IconButton>
                          </>
                        }
                      >
                        <IconButton edge="start">
                          <Icon.DragHandleIcon />
                        </IconButton>
                        책장 {index}
                      </Title>
                      <Showcase>
                        {Array.from({ length: 10 }).map(() => {
                          return <ConstrainedImgBox img={DummyImg1} height="170px" />;
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
