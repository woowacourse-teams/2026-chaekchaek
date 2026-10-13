import { Link } from 'react-router-dom';

import { landingImages } from '../../imgs';
import styles from './LandingHeader.module.css';

export const LandingHeader = () => (
  <header className={styles.header}>
    <Link className={styles.brand} to="/landing" aria-label="책췍 랜딩 홈">
      <img src={landingImages.chaekchaekCharacter} alt="" />
      <span>책췍</span>
    </Link>
  </header>
);
