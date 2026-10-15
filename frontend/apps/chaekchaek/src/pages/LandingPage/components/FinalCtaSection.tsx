import { landingImages } from '../imgs';
import styles from '../LandingPage.module.css';
import { LandingCtaLink } from './LandingCtaLink/index';
export const FinalCtaSection = () => (
  <section className={styles.finalCta}>
    <div>
      <img src={landingImages.chaekchaekCharacter} alt="책 위에 앉은 책췍 참새" />
      <h2>당신은 어떤 생각을 했나요?</h2>
      <p>읽은 책에 대한 당신의 이야기를 책췍에서 들려주세요.</p>
      <LandingCtaLink to="/intro">책 방문하러 가기　→</LandingCtaLink>
    </div>
    <footer>
      <span>책췍 CHAEKCHAEK</span>
      <span>© 2026 책췍</span>
    </footer>
  </section>
);
