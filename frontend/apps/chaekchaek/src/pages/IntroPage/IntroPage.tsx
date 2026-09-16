import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { PopularBooks } from './components/PopularBooks';

import './IntroPage.css';

export const IntroPage = () => {
  return (
    <Layout>
      <Header />
      <Main>
        <PopularBooks />
      </Main>
    </Layout>
  );
};
