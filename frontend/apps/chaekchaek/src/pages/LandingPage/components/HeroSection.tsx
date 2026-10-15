import { HeroBookshelf } from './HeroBookshelf/index';
import { LandingCtaLink } from './LandingCtaLink';
import { LandingHeader } from './LandingHeader';

import styles from '../LandingPage.module.css';

export const HeroSection = () => (
  <section className={styles.hero}>
    <LandingHeader />
    <div className={styles.heroCopy}>
      <p className={styles.eyebrow}>READ · RECORD · DISCOVER</p>
      <h1>같은 책, 다른 시선</h1>
      <p>
        같은 책을 읽어도, 우리는 서로 다른 생각을 합니다.
        <br />
        책췍에서 다른 사람의 생각을 발견하고, 나의 감상을 나눠보세요.
      </p>
      <LandingCtaLink className={styles.outlineButton} to="/intro">
        책췍 시작하기　→
      </LandingCtaLink>
    </div>
    <HeroBookshelf />
  </section>
);
