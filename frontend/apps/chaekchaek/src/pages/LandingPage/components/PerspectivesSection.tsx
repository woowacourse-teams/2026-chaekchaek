import { landingImages } from '../imgs';
import styles from '../LandingPage.module.css';
import { LandingCtaLink } from './LandingCtaLink';
const thoughts = [
  '무심해 보였다는 클라마르 소녀에게서 나르게 바라는 물음은 없다고 생각하고, 그 사람만이 가진 철학을 이해하며 같이 살아가면 좋지 않을까 생각했다.',
  '뫼르소는 죽음의 순간엔 이미 “잘게 퍼붓듯한 적의”에, 마음이나 상황은 수동적이었으며 고독도 행동 자체는 결코 순수한 방향이 아닌 능동적인 공포의 성격을 띤다.',
  '사회가 정해한 사랑이라는 단어에 내가 느끼는 감정을 억지로 끼워 맞추려는 시도가 꼭 필요한가 싶다.',
];
export const PerspectivesSection = () => (
  <section className={styles.perspectives}>
    <header className={styles.sectionHeader}>
      <p className={styles.eyebrow}>01 DIFFERENT PERSPECTIVES</p>
      <div>
        <h2>
          같은 책을 읽고,
          <br />
          우리는 얼마나 다른 생각을 할까요?
        </h2>
        <p>
          감상을 짧게 기록하고, 다른 독자들이 남긴 감상을 만나보세요.
          <br />
          같은 책을 읽어도 저마다 다른 생각을 발견할 수 있어요.
        </p>
      </div>
    </header>
    <div className={styles.perspectiveVisual}>
      <div className={styles.bookMessage}>
        <img src={landingImages.stranger} alt="이방인 책 표지" loading="lazy" />
        <p>
          한 권의 책,
          <br />
          서로 다른 마음.
        </p>
      </div>
      <div className={styles.thoughts}>
        {thoughts.map((text, index) => (
          <article key={text}>
            <small>🐦 책췍 독자 {index + 1}</small>
            <p>{text}</p>
            <footer>♡ 좋아요　□ 답글</footer>
          </article>
        ))}
        <LandingCtaLink to="/feed">다른 감상 둘러보기 →</LandingCtaLink>
      </div>
    </div>
  </section>
);
