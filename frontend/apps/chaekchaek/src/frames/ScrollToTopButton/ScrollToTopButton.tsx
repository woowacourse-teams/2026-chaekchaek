import { FloatingButton, Icon } from '@chaekchaek/design-system';

import styles from './ScrollToTopButton.module.css';

export const ScrollToTopButton = () => {
  const handleClick = () => {
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  };

  return (
    <FloatingButton variant="primary" onClick={handleClick}>
      <Icon.LongArrowUpIcon color="inverse" />
    </FloatingButton>
  );
};
