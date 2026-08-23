import type { ReadingArchiveProps } from './ReadingArchive.types';
import styles from './ReadingArchive.module.css';

export const ReadingArchive = (_props: ReadingArchiveProps) => (
  <div className={styles.root} aria-hidden="true">
    <div className={styles.backBook} />
    <div className={styles.frontBook}>
      <span className={styles.orangeLine} />
      <span className={styles.grayLine} />
    </div>
    <span className={styles.label}>READING ARCHIVE</span>
  </div>
);
