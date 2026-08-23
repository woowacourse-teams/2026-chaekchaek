import type { LoginHeroProps } from './LoginHero.types';

import styles from './LoginHero.module.css';

export const LoginHero = ({ reverse }: LoginHeroProps) => (
  <header className={`${styles.root} ${reverse ? styles.reverse : ''}`}>
    <h1 className={styles.title}>반가워요!</h1>
    <p className={styles.description}>책췍과 함께 독서 여정을 시작해보세요.</p>
  </header>
);
