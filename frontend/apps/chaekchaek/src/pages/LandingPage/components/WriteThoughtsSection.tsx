import { useEffect, useRef } from 'react';

import { landingImages } from '../imgs';
import styles from '../LandingPage.module.css';
import { LandingCtaLink } from './LandingCtaLink/index';

export const WriteThoughtsSection = () => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      videoRef.current?.pause();
    }
  }, []);

  return (
    <section className={styles.writeSection}>
      <div className={styles.sideCopy}>
        <p className={styles.eyebrow}>02 / WRITE YOUR THOUGHTS</p>
        <h2>
          읽으며 떠오른 생각을
          <br />
          자유롭게 남겨보세요.
        </h2>
        <p>인상 깊었던 문장부터 짧은 생각까지, 책췍에서 나만의 감상을 기록하고 나눠보세요.</p>
        <LandingCtaLink to="/books">감상 남기기 →</LandingCtaLink>
      </div>
      <video
        ref={videoRef}
        className={styles.demo}
        autoPlay
        muted
        loop
        playsInline
        preload="metadata"
        poster={landingImages.reviewDemoPoster}
        aria-label="감상 작성 화면 시연"
      >
        <source src="/landing/review-demo.mp4" type="video/mp4" />
      </video>
      <img
        className={styles.mobileDemo}
        src={landingImages.mobileReview}
        alt="책췍 모바일 감상 작성 화면"
      />
    </section>
  );
};
