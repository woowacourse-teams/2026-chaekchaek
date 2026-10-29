import { useRef, useState } from 'react';

import { landingImages } from '../../imgs';
import { AuthStory } from './AuthStory';
import { CacheStory } from './CacheStory';
import { FeedStory } from './FeedStory';
import { feedCovers } from './feedCollage';
import { technologySlides } from './technologySlides';
import styles from './TechnologySection.module.css';

const number = (index: number) => String(index + 1).padStart(2, '0');

export const TechnologySection = () => {
  const [current, setCurrent] = useState(0);
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);
  const move = (index: number) => {
    setCurrent((index + technologySlides.length) % technologySlides.length);
  };

  return (
    <section
      className={styles.section}
      aria-labelledby="technology-heading"
      aria-roledescription="캐러셀"
    >
      <header className={styles.header}>
        <p className={styles.eyebrow}>04 / HOW WE BUILT IT</p>
        <div>
          <h2 id="technology-heading">책책을 만드는 기술</h2>
          <p>더 나은 독서 경험을 위해 고민한 세 가지 이야기입니다.</p>
        </div>
      </header>
      <div className={styles.layout}>
        <div
          className={styles.tabs}
          role="tablist"
          aria-label="기술 소개 선택"
          aria-orientation="vertical"
        >
          {technologySlides.map((item, index) => (
            <button
              key={item.id}
              ref={(element) => {
                tabs.current[index] = element;
              }}
              type="button"
              role="tab"
              id={`technology-tab-${item.id}`}
              aria-selected={current === index}
              aria-controls={`technology-panel-${item.id}`}
              tabIndex={current === index ? 0 : -1}
              onClick={() => move(index)}
              onKeyDown={(event) => {
                let target: number;
                if (event.key === 'ArrowDown' || event.key === 'ArrowRight')
                  target = (index + 1) % technologySlides.length;
                else if (event.key === 'ArrowUp' || event.key === 'ArrowLeft')
                  target = (index - 1 + technologySlides.length) % technologySlides.length;
                else if (event.key === 'Home') target = 0;
                else if (event.key === 'End') target = technologySlides.length - 1;
                else return;
                event.preventDefault();
                move(target);
                tabs.current[target]?.focus();
              }}
            >
              <span className={styles.menuNumber}>{number(index)}</span>
              {index !== 2 ? (
                <img
                  className={styles.menuBook}
                  src={index === 0 ? landingImages.stranger : feedCovers[0].src}
                  alt=""
                />
              ) : (
                <span className={`${styles.menuPreview} ${styles.authPreview}`} aria-hidden="true">
                  <img src={landingImages.mobileLibrary} alt="" loading="lazy" />
                </span>
              )}
              <span className={styles.menuCopy}>
                <strong>{item.id.toUpperCase()}</strong>
                <span>{item.label}</span>
              </span>
            </button>
          ))}
        </div>
        <div className={styles.panels}>
          {technologySlides.map((item, index) => (
            <article
              key={item.id}
              id={`technology-panel-${item.id}`}
              role="tabpanel"
              aria-labelledby={`technology-tab-${item.id}`}
              aria-hidden={current !== index}
              inert={current !== index}
              tabIndex={current === index ? 0 : -1}
              className={`${styles.panel} ${styles.cachePanel} ${current === index ? styles.active : ''}`}
            >
              {index === 0 ? <CacheStory /> : index === 1 ? <FeedStory /> : <AuthStory />}
            </article>
          ))}
        </div>
      </div>
    </section>
  );
};
