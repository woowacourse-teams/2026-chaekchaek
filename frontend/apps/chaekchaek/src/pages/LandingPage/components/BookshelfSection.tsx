import { landingImages } from '../imgs';
import styles from '../LandingPage.module.css';
import { LandingCtaLink } from './LandingCtaLink/index';

export const BookshelfSection = () => (
  <section className={styles.bookshelfSection}>
    <div className={styles.sideCopy}>
      <p className={styles.eyebrow}>03 / SHARE YOUR BOOKSHELF</p>
      <h2>
        내가 읽는 책으로,
        <br />
        나를 보여주세요.
      </h2>
      <p>
        읽고 싶은 책을 컬렉션 서재에 담아보세요.
        <br />
        책들이 하나둘 쌓이면 나만의 서재가 완성돼요.
      </p>
      <LandingCtaLink className={styles.outlineButton} to="/login">
        나만의 서재 만들기　→
      </LandingCtaLink>
    </div>
    <img
      className={styles.mobileShelf}
      src={landingImages.mobileLibrary}
      alt="책췍 모바일 내 서재 화면"
    />
    <div className={styles.shelfExhibition}>
      <div className={styles.coverRow}>
        {landingImages.attachments.map((image) => (
          <img key={image} src={image} alt="" loading="lazy" />
        ))}
      </div>
      <div className={styles.collection}>
        <p className={styles.eyebrow}>A SHELF OF TASTE · 09 BOOKS</p>
        <h3>
          한 권씩 쌓인 취향이
          <br />
          나만의 서재가 됩니다.
        </h3>
        <img src={landingImages.bookshelfList} alt="공개 서재 목록" loading="lazy" />
      </div>
    </div>
  </section>
);
