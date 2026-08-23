import type { LoginHeroProps } from './LoginHero.types';

import styles from './LoginHero.module.css';

export const LoginHero = (_props: LoginHeroProps) => (
  <header className={styles.root}>
    <h1 className={styles.title}>반가워요!</h1>
    <p className={styles.description}>책췍과 함께 독서 여정을 시작해보세요.</p>
  </header>
);
