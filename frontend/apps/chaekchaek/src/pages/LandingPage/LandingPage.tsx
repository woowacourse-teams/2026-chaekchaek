import { BookshelfSection } from './components/BookshelfSection';
import { FinalCtaSection } from './components/FinalCtaSection';
import { HeroSection } from './components/HeroSection';
import { PerspectivesSection } from './components/PerspectivesSection';
import { TechnologySection } from './components/TechnologySection';
import { WriteThoughtsSection } from './components/WriteThoughtsSection';
import styles from './LandingPage.module.css';

export const LandingPage = () => (
  <main className={styles.page}>
    <HeroSection /><PerspectivesSection /><WriteThoughtsSection /><BookshelfSection /><TechnologySection /><FinalCtaSection />
  </main>
);
