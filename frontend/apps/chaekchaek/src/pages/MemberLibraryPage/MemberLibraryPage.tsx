import styles from './MemberLibraryPage.module.css';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

export const MemberLibraryPage = () => {
  return (
    <Layout>
      <Header />
      <Main>
        <Container></Container>
      </Main>
    </Layout>
  );
};
