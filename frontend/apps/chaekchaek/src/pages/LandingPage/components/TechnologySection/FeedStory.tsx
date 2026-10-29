import { feedCovers, feedThoughts } from './feedCollage';
import { technologySlides } from './technologySlides';
import styles from './TechnologySection.module.css';

export const FeedStory = () => {
  const feed = technologySlides[1];
  const [opening, closing] = feed.title.split(', ');

  return (
    <div className={styles.feedStory}>
      <div className={styles.feedCopy}>
        <div className={styles.intro}>
          <p className={styles.eyebrow}>02 / FEED</p>
          <h3>
            <span>{opening},</span> <span>{closing}</span>
          </h3>
          <p className={styles.label}>문제 상황</p>
          <p className={styles.body}>{feed.problem}</p>
        </div>
        <div className={styles.solution}>
          <p className={styles.label}>기술적 해결</p>
          <p className={styles.solutionTitle}>{feed.solution}</p>
          <ul>
            {feed.details.map((detail) => (
              <li key={detail}>{detail}</li>
            ))}
          </ul>
        </div>
      </div>
      <div
        className={styles.feedCollage}
        role="group"
        aria-label="책 표지와 랜딩페이지 독자의 감상 모음"
      >
        <blockquote className={styles.thoughtAbove}>
          <span className={styles.quoteMark} aria-hidden="true">
            “
          </span>
          <p>{feedThoughts[0]}</p>
        </blockquote>
        <div className={styles.collageCovers}>
          {feedCovers.map((cover) => (
            <img
              key={cover.title}
              src={cover.src}
              alt={`${cover.title} 책 표지`}
              width={cover.width}
              height={cover.height}
              loading="lazy"
            />
          ))}
        </div>
        <blockquote className={styles.thoughtBelow}>
          <span className={styles.quoteMark} aria-hidden="true">
            “
          </span>
          <p>{feedThoughts[1]}</p>
        </blockquote>
        <div className={styles.thoughtContinuation} aria-hidden="true">
          <span>·</span>
          <span>·</span>
          <span>·</span>
        </div>
      </div>
      <div className={styles.feedResults}>
        <div className={styles.cacheComparison}>
          <div>
            <span className={styles.caption}>BEFORE · 홈</span>
            <strong>{feed.before?.value}</strong>
            <p>{feed.before?.description}</p>
          </div>
          <span className={styles.arrow} aria-hidden="true">
            →
          </span>
          <div className={styles.after}>
            <span className={styles.caption}>AFTER · 피드</span>
            <strong>{feed.after?.value}</strong>
            <p>{feed.after?.description}</p>
          </div>
        </div>
        <p className={styles.feedOutcome}>{feed.outcome}</p>
      </div>
    </div>
  );
};
