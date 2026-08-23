import { createClassName } from '@chaekchaek/design-system';

import profileIcon from './imgs/header-action.svg';
import styles from './Header.module.css';

import type { ProfileButtonProps } from './ProfileButton.types';

const classnameDefault = 'frame-Header-ProfileButton';

export const ProfileButton = ({
  className,
  type = 'button',
  ...restProps
}: ProfileButtonProps) => {
  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers: {},
    className,
  });

  return (
    <button type={type} className={classname} aria-label="프로필" {...restProps}>
      <img src={profileIcon} alt="" />
    </button>
  );
};
