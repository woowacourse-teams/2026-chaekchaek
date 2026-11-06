import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { PopularBooks } from './components/PopularBooks';
import { LatestReviews } from './components/LatestReviews';

import styles from './IntroPage.module.css';

export const IntroPage = () => {
  return (
    <Layout className={styles[`intro-page`]}>
      <Header />
      <Main className={styles.wrap}>
        <div className={styles[`intro-left-side`]}>
          <PopularBooks />
        </div>
        <div className={styles[`intro-right-side`]}>
          <LatestReviews />
        </div>
      </Main>
    </Layout>
  );
};
