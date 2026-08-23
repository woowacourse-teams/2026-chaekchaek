import { Layout, Main } from '@/frames';

import { LoginHero } from './components/LoginHero';
import { ReadingArchive } from './components/ReadingArchive';
import { SocialLoginButton } from './components/SocialLoginButton';

import styles from './LoginPage.module.css';

export const LoginPage = () => {
  return (
    <Layout className={styles.page}>
      <Main className={styles.main}>
        <LoginHero />
        <div className={styles.loginButtons}>
          {/* 
            <SocialLoginButton provider="kakao" />
            <SocialLoginButton provider="apple" /> 
          */}
          <SocialLoginButton provider="google" />
        </div>
        <ReadingArchive />
      </Main>
    </Layout>
  );
};
