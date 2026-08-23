import { createClassName } from '@chaekchaek/design-system';

import { ENV } from '@/configs/env';

import profileIcon from './imgs/header-action.svg';
import styles from './Header.module.css';

import type { ProfileButtonProps } from './ProfileButton.types';

const classnameDefault = 'frame-Header-ProfileButton';

export const ProfileButton = ({ className, type = 'button', ...restProps }: ProfileButtonProps) => {
  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers: {},
    className,
  });

  const handleMove = () => {
    window.location.href = `${ENV.APP_API_URL}/oauth2/authorization/google`;
  };

  return (
    <button
      type={type}
      className={classname}
      aria-label="프로필"
      onClick={handleMove}
      {...restProps}
    >
      <img src={profileIcon} alt="" />
    </button>
  );
};
