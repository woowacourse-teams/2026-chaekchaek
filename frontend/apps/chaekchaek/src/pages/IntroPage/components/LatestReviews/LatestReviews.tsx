import { Button, Icon } from '@chaekchaek/design-system';
import styles from './LatestReviews.module.css';

export const LatestReviews = () => {
  return (
    <div className={styles['scene-latest-reviews']}>
      <div className={styles['latest-reviews-title']}>
        <h1>방금 남겨진 문장</h1>
        <div className="right">
          <Button shape="link" variant="ghost" trailing={<Icon.LongArrowRightIcon />}>
            모두보기
          </Button>
        </div>
      </div>
    </div>
  );
};
