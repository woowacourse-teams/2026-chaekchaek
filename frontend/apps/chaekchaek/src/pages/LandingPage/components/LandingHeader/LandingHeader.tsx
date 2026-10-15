import { LandingCtaLink } from '../LandingCtaLink/index';
import { landingImages } from '../../imgs';
import styles from './LandingHeader.module.css';

export const LandingHeader = () => (
  <header className={styles.header}>
    <LandingCtaLink className={styles.brand} to="/landing" aria-label="책췍 랜딩 홈">
      <img src={landingImages.chaekchaekCharacter} alt="" />
      <span>책췍</span>
    </LandingCtaLink>
  </header>
);
