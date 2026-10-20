import styles from './LibraryPage.module.css';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

export const LibraryPage = () => {
  return (
    <Layout>
      <Header />
      <Main>
        <Container></Container>
      </Main>
    </Layout>
  );
};
