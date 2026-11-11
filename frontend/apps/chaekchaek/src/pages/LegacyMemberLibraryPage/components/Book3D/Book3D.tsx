import { useState, type CSSProperties } from 'react';

import { ImageGradientBackground, ResponsiveText } from '@chaekchaek/design-system';

import styles from './Book3D.module.css';

import cover from './imgs/dummy1-cover.jpeg';
import spine from './imgs/dummy1-side.jpeg';
import back from './imgs/dummy1-back.jpeg';

import type { Book3DProps } from './Book3D.types';

export const Book3D = ({
  title = '마션 (샘플)',
  coverImageUrl = cover,
  spineImageUrl = spine,
  backImageUrl = back,
  height = 300,
  className,
}: Book3DProps) => {
  const [isBackVisible, setIsBackVisible] = useState(false);
  const bookHeight = Number.isFinite(height) && height > 0 ? height : 300;
  const style = {
    '--book-preferred-height': `${bookHeight}px`,
  } as CSSProperties;

  return (
    <div
      className={[styles.scene, className].filter(Boolean).join(' ')}
      style={style}
      data-back={isBackVisible}
      aria-label={`${title} 뒷표지 보기`}
      aria-pressed={isBackVisible}
      onClick={() => setIsBackVisible((previous) => !previous)}
    >
      <span className={styles.book} aria-hidden="true">
        <span className={`${styles.face} ${styles.front}`}>
          <img src={coverImageUrl} alt="" draggable={false} />
        </span>
        <span className={`${styles.face} ${styles.back}`}>
          <img src={backImageUrl} alt="" draggable={false} />
        </span>
        <span className={`${styles.face} ${styles.spine}`}>
          {spineImageUrl && <img src={spineImageUrl} alt="" draggable={false} />}

          {!spineImageUrl && (
            <ImageGradientBackground img={coverImageUrl}>
              <ResponsiveText sx={{ mt: 4 }} direction="vertical">
                {title}
              </ResponsiveText>
            </ImageGradientBackground>
          )}
        </span>
        <span className={`${styles.face} ${styles.top}`} />
      </span>
    </div>
  );
};
