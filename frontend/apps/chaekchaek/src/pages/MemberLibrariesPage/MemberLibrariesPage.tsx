import styles from './MemberLibrariesPage.module.css';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';
import { Container } from '@/frames/Container';

export const MemberLibrariesPage = () => {
  return (
    <Layout>
      <Header />
      <Main>
        <Container></Container>
      </Main>
    </Layout>
  );
};
