import { useEffect, useLayoutEffect, useRef, useState } from 'react';

import { LandingCtaLink } from '../LandingCtaLink/index';
import { landingImages } from '../../imgs';

import {
  getBook,
  getBookLayout,
  getTransitionDuration,
  AUTOPLAY_SWEEP,
  AUTOPLAY_TIMING,
  INITIAL_INDEX,
  SHELF_BUFFER,
  shelfBooks,
} from './bookshelf';
import type { BookStyle } from './HeroBookshelf.types';
import styles from './HeroBookshelf.module.css';

export const HeroBookshelf = () => {
  const viewportRef = useRef<HTMLDivElement>(null);
  const autoplayStarted = useRef(false);
  const resumeDelay = useRef(false);
  const [scale, setScale] = useState(1);
  const [reducedMotion, setReducedMotion] = useState(
    () => window.matchMedia('(prefers-reduced-motion: reduce)').matches,
  );
  const [activeIndex, setActiveIndex] = useState(INITIAL_INDEX);
  const [previousIndex, setPreviousIndex] = useState<number | null>(null);
  const [direction, setDirection] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [settledIndex, setSettledIndex] = useState(INITIAL_INDEX);

  useLayoutEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    const resize = () =>
      setScale(
        Math.min(1, Math.max(0.48, viewport.clientWidth / 1100), viewport.clientHeight / 475),
      );
    resize();
    const observer = new ResizeObserver(resize);
    observer.observe(viewport);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const media = window.matchMedia('(prefers-reduced-motion: reduce)');
    const update = () => setReducedMotion(media.matches);
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, []);

  useEffect(() => {
    if (reducedMotion || isPaused || isAnimating) return;
    const atTurnaround =
      activeIndex === AUTOPLAY_SWEEP.minIndex || activeIndex === AUTOPLAY_SWEEP.maxIndex;
    const delay = !autoplayStarted.current
      ? AUTOPLAY_TIMING.initialDelay
      : atTurnaround || resumeDelay.current
        ? AUTOPLAY_TIMING.turnaroundHold
        : AUTOPLAY_TIMING.betweenBooks;
    const timer = setTimeout(() => {
      const nextDirection =
        activeIndex >= AUTOPLAY_SWEEP.maxIndex
          ? -1
          : activeIndex <= AUTOPLAY_SWEEP.minIndex
            ? 1
            : direction || 1;
      autoplayStarted.current = true;
      resumeDelay.current = false;
      setPreviousIndex(activeIndex);
      setDirection(nextDirection);
      setActiveIndex(activeIndex + nextDirection);
      setIsAnimating(true);
    }, delay);
    return () => clearTimeout(timer);
  }, [activeIndex, direction, isAnimating, isPaused, reducedMotion]);

  useEffect(() => {
    if (!isAnimating) return;
    const duration = reducedMotion ? 0 : getTransitionDuration(direction) + 50;
    const timer = setTimeout(() => {
      setSettledIndex(activeIndex);
      setIsAnimating(false);
      setPreviousIndex(null);
    }, duration);
    return () => clearTimeout(timer);
  }, [activeIndex, direction, isAnimating, reducedMotion]);

  const selectedBook = getBook(settledIndex);
  const selectedNumber =
    (((settledIndex % shelfBooks.length) + shelfBooks.length) % shelfBooks.length) + 1;
  const indices = Array.from(
    { length: SHELF_BUFFER * 2 + 1 },
    (_, offset) => activeIndex + offset - SHELF_BUFFER,
  );

  return (
    <div className={styles.shelf} role="region" aria-label="책장 둘러보기">
      <div ref={viewportRef} className={styles.viewport} aria-hidden="true">
        <div className={styles.track} style={{ transform: `scale(${scale})` }}>
          {indices.map((index) => {
            const book = getBook(index);
            const slot = index - activeIndex;
            const layout = getBookLayout(index, activeIndex, direction);
            const role =
              slot === 0 ? 'incoming' : index === previousIndex ? 'outgoing' : 'neighbor';
            const duration = getTransitionDuration(direction);
            const style: BookStyle = {
              width: book.width,
              height: book.height,
              zIndex: role === 'incoming' ? 100 : role === 'outgoing' ? 90 : 50 - Math.abs(slot),
              '--book-x': `${layout.x}px`,
              '--book-y-rotation': `${layout.rotateY}deg`,
              '--book-z-rotation': `${layout.rotateZ}deg`,
              '--book-origin': `${layout.origin} bottom`,
              '--book-spine-width': `${book.spineWidth}px`,
              '--book-color': book.color,
              '--book-ink': book.ink,
              '--book-duration': `${duration}ms`,
              '--book-delay': `${role === 'neighbor' ? Math.min(Math.abs(slot) * 14, 50) : 0}ms`,
              '--book-marker-x': `${layout.markerX}px`,
            };
            return (
              <div
                key={index}
                className={styles.book}
                style={style}
                data-slot={slot}
                data-role={role}
                data-title={book.title}
              >
                {book.favorite && (
                  <span className={styles.marker} aria-hidden="true">
                    ♥
                  </span>
                )}
                <span className={styles.tilt} aria-hidden="true">
                  <span className={styles.body}>
                    <span className={styles.cover}>
                      {book.cover ? (
                        <img src={book.cover} alt="" draggable={false} />
                      ) : (
                        <span className={styles.typeCover}>
                          <small>
                            CHAEKCHAEK
                            <br />
                            READING COLLECTION
                          </small>
                          <strong>{book.title}</strong>
                          <span className={styles.coverRule} />
                          <span>{book.author}</span>
                          <small>한 권의 책, 서로 다른 생각.</small>
                        </span>
                      )}
                    </span>
                    {(['left', 'right'] as const).map((side) => (
                      <span
                        key={side}
                        className={`${styles.spine} ${side === 'left' ? styles.leftSpine : styles.rightSpine} ${book.cover ? styles.strangerSpine : ''}`}
                      >
                        {book.cover ? (
                          <>
                            <small className={styles.classic}>
                              클래식
                              <br />
                              005
                            </small>
                            <strong>이방인</strong>
                            <span className={styles.spineAuthor}>알베르 카뮈</span>
                            <span
                              className={styles.spinePortrait}
                              style={{ backgroundImage: `url(${landingImages.stranger})` }}
                            />
                            <small className={styles.publisher}>시간과공간사</small>
                          </>
                        ) : (
                          <>
                            <small>
                              {String(
                                (((index % shelfBooks.length) + shelfBooks.length) %
                                  shelfBooks.length) +
                                  1,
                              ).padStart(2, '0')}
                            </small>
                            <strong>{book.title}</strong>
                            <span className={styles.spineAuthor}>{book.author}</span>
                            <small>책췍</small>
                          </>
                        )}
                      </span>
                    ))}
                  </span>
                </span>
              </div>
            );
          })}
        </div>
      </div>
      <LandingCtaLink className={styles.note} to="/books/9791190818490">
        <b>이방인 · 독서 감상 04</b>
        <p>
          “타인의 시선이 만든 현실에서
          <br />한 번 벗어나 세상을 바라보기.”
        </p>
        <small>다른 독자의 감상 이어보기 ↗</small>
      </LandingCtaLink>
      <div className={styles.footer}>
        <div className={styles.selection} role="status" aria-live="off">
          <small>
            SELECTED {String(selectedNumber).padStart(2, '0')} / {shelfBooks.length}
          </small>
          <span>
            {selectedBook.title}
            <span className={styles.author}>{selectedBook.author}</span>
          </span>
        </div>
        {!reducedMotion && (
          <button
            type="button"
            className={styles.playbackButton}
            aria-label={isPaused ? '애니메이션 재생' : '애니메이션 일시정지'}
            title={isPaused ? '애니메이션 재생' : '애니메이션 일시정지'}
            onClick={() => {
              autoplayStarted.current = true;
              resumeDelay.current = true;
              setIsPaused((paused) => !paused);
            }}
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor" aria-hidden="true">
              {isPaused ? (
                <path d="M3 1.5 12 7l-9 5.5z" />
              ) : (
                <>
                  <rect x="3" y="2" width="2.5" height="10" />
                  <rect x="8.5" y="2" width="2.5" height="10" />
                </>
              )}
            </svg>
          </button>
        )}
      </div>
      <LandingCtaLink
        className={styles.reviewLink}
        to={selectedBook.cover ? '/books/9791190818490' : '/books'}
      >
        다른 독자의 감상 이어보기 ↗
      </LandingCtaLink>
    </div>
  );
};
