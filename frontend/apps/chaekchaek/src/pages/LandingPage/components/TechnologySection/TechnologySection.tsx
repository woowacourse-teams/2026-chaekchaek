import { useRef, useState } from 'react';

import { technologySlides } from './technologySlides';
import styles from './TechnologySection.module.css';

const number = (index: number) => String(index + 1).padStart(2, '0');

export const TechnologySection = () => {
  const [current, setCurrent] = useState(0);
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);
  const slide = technologySlides[current];
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
        <p className={styles.eyebrow}>04 / BEHIND THE EXPERIENCE</p>
        <div>
          <h2 id="technology-heading">더 나은 독서 경험을 위한 기술</h2>
          <p>책책의 경험 뒤에는 이런 고민과 기술적 선택이 있습니다.</p>
        </div>
      </header>
      <div className={styles.tabs} role="tablist" aria-label="기술 소개 선택">
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
              if (event.key === 'ArrowRight') target = (index + 1) % technologySlides.length;
              else if (event.key === 'ArrowLeft')
                target = (index - 1 + technologySlides.length) % technologySlides.length;
              else if (event.key === 'Home') target = 0;
              else if (event.key === 'End') target = technologySlides.length - 1;
              else return;
              event.preventDefault();
              move(target);
              tabs.current[target]?.focus();
            }}
          >
            <span>{number(index)}</span>
            {item.label}
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
            className={`${styles.panel} ${styles.slide} ${current === index ? styles.active : ''}`}
          >
            <div className={styles.copy}>
              <div className={styles.intro}>
                <p className={styles.eyebrow}>
                  {number(index)} / {item.category}
                </p>
                <h3>{item.title}</h3>
                <p className={styles.label}>문제 상황</p>
                <p className={styles.body}>{item.problem}</p>
              </div>
              <div className={styles.solution}>
                <p className={styles.label}>기술적 해결</p>
                <p className={styles.solutionTitle}>{item.solution}</p>
                <ul>
                  {item.details.map((detail) => (
                    <li key={detail}>{detail}</li>
                  ))}
                </ul>
              </div>
            </div>
            <div className={styles.results}>
              <p className={styles.label}>달라진 경험</p>
              {item.before && item.after && (
                <div className={styles.comparison}>
                  <div>
                    <span className={styles.caption}>BEFORE</span>
                    <strong>{item.before.value}</strong>
                    <p>{item.before.description}</p>
                  </div>
                  <span className={styles.arrow} aria-hidden="true">
                    →
                  </span>
                  <div className={styles.after}>
                    <span className={styles.caption}>AFTER</span>
                    <strong>{item.after.value}</strong>
                    <p>{item.after.description}</p>
                  </div>
                </div>
              )}
              {item.flows && (
                <div className={styles.flows}>
                  {item.flows.map((flow) => (
                    <div key={flow.label}>
                      <strong>{flow.label}</strong>
                      <ol>
                        {flow.steps.map((step) => (
                          <li key={step}>{step}</li>
                        ))}
                      </ol>
                    </div>
                  ))}
                </div>
              )}
              <div className={styles.resultNotes}>
                <p className={styles.outcome}>{item.outcome}</p>
                {item.note && <p className={styles.note}>{item.note}</p>}
              </div>
            </div>
          </article>
        ))}
      </div>
      <footer className={styles.controls}>
        <p aria-live="polite" aria-atomic="true">
          <span className={styles.current}>{number(current)}</span> / 03
          <span className={styles.srOnly}> · {slide.label}</span>
        </p>
        <div>
          <button type="button" onClick={() => move(current - 1)} aria-label="이전 기술 소개">
            <span aria-hidden="true">←</span>
          </button>
          <button type="button" onClick={() => move(current + 1)} aria-label="다음 기술 소개">
            <span aria-hidden="true">→</span>
          </button>
        </div>
      </footer>
    </section>
  );
};
