import { createClassName } from '@chaekchaek/design-system';

import profileIcon from './imgs/header-action.svg';
import styles from './Header.module.css';

import type { ProfileButtonProps } from './ProfileButton.types';

import { getOauthLoginUrl } from '@/auth/oauth';

const classnameDefault = 'frame-Header-ProfileButton';

export const ProfileButton = ({ className, type = 'button', ...restProps }: ProfileButtonProps) => {
  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers: {},
    className,
  });

  const handleMove = () => {
    window.location.href = getOauthLoginUrl('google');
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
