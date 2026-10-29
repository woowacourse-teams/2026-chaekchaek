import { landingImages } from '../../imgs';
import { technologySlides } from './technologySlides';
import styles from './TechnologySection.module.css';

export const CacheStory = () => {
  const cache = technologySlides[0];

  return (
    <div className={styles.cacheStory}>
      <div className={styles.cacheCopy}>
        <p className={styles.eyebrow}>01 / CACHE</p>
        <h3>{cache.title}</h3>
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
        <img
          src={landingImages.stranger}
          alt="알베르 카뮈의 이방인 책 표지"
          width="693"
          height="1080"
          loading="lazy"
        />
      </div>
      <div className={styles.cacheResults}>
        <div className={styles.cacheComparison}>
          <div>
            <span className={styles.caption}>BEFORE</span>
            <strong>{cache.before?.value}</strong>
          </div>
          <span className={styles.arrow} aria-hidden="true">
            →
          </span>
          <div className={styles.after}>
            <span className={styles.caption}>AFTER</span>
            <strong>{cache.after?.value}</strong>
          </div>
          <p>감상 수·답글 수 집계 쿼리 각각의 실행 횟수입니다.</p>
        </div>
        <div className={styles.cacheNotes}>
          <p className={styles.cacheHit}>{cache.outcome}</p>
          <p className={styles.note}>{cache.note}</p>
        </div>
      </div>
    </div>
  );
};
