import { landingImages } from '../../imgs';
// Same cover as the supplied reference: https://image.yes24.com/goods/143911524/XL
import honmono from './imgs/honmono.jpg';
import { technologySlides } from './technologySlides';
import styles from './TechnologySection.module.css';

export const CacheStory = () => {
  const cache = technologySlides[0];
  const [opening, closing] = cache.title.split(', ');

  return (
    <div className={styles.cacheStory}>
      <div className={styles.cacheCopy}>
        <p className={styles.eyebrow}>01 / CACHE</p>
        <h3>
          <span>{opening},</span> <span>{closing}</span>
        </h3>
        <p className={styles.body}>{cache.problem}</p>
        <div className={styles.cacheSolution}>
          <h4>{cache.solution}</h4>
          <ul>
            {cache.details.map((detail) => (
              <li key={detail}>{detail}</li>
            ))}
          </ul>
        </div>
      </div>
      <div className={styles.bookVisual}>
        <div className={styles.searchPreview} aria-hidden="true">
          <span>⌕</span> 책 제목, 작가를 검색해보세요.
        </div>
        <div className={styles.bookStack}>
          <img
            className={styles.backBook}
            src={honmono}
            alt="성해나의 혼모노 책 표지"
            width="817"
            height="1200"
            loading="lazy"
          />
          <img
            className={styles.frontBook}
            src={landingImages.stranger}
            alt="알베르 카뮈의 이방인 책 표지"
            width="693"
            height="1080"
            loading="lazy"
          />
        </div>
      </div>
      <div className={styles.cacheResults}>
        <div className={styles.cacheComparison}>
          <div>
            <span className={styles.caption}>BEFORE</span>
            <strong>{cache.before?.value}</strong>
            <p>감상 수·답글 수 집계 쿼리 각각 실행</p>
          </div>
          <span className={styles.arrow} aria-hidden="true">
            →
          </span>
          <div className={styles.after}>
            <span className={styles.caption}>AFTER</span>
            <strong>{cache.after?.value}</strong>
            <p>감상 수·답글 수 집계 쿼리 각각 실행</p>
          </div>
        </div>
        <div className={styles.cacheNotes}>
          <div className={styles.cacheBadges}>
            <span>TTL 30분</span>
            <span>최대 500개</span>
            <span>{cache.outcome}</span>
          </div>
          <p className={styles.note}>{cache.note}</p>
        </div>
      </div>
    </div>
  );
};
