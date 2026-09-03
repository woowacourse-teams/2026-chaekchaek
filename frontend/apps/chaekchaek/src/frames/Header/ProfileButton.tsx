import { createClassName } from '@chaekchaek/design-system';

import { getOauthLoginUrl } from '@/auth/oauth';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { useExecute } from '@/services/core/useExecute';
import { postAuthOauth2GuestContext } from '@/services/apis/authOauth2GuestContext/repository';

import profileIcon from './imgs/header-action.svg';
import styles from './Header.module.css';

import type { ProfileButtonProps } from './ProfileButton.types';

const oauthProvider = 'google';

const classnameDefault = 'frame-Header-ProfileButton';

export const ProfileButton = ({ className, type = 'button', ...restProps }: ProfileButtonProps) => {
  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers: {},
    className,
  });

  const { guest } = useAuthContext();

  const { mutate: postAuthOauth2GuestContextMutate } = useExecute({
    executeFn: postAuthOauth2GuestContext,
    onSuccess: () => {
      window.location.href = getOauthLoginUrl(oauthProvider);
    },
  });

  const handleClick = async () => {
    if (!guest) {
      window.location.href = getOauthLoginUrl(oauthProvider);
      return;
    }

    await postAuthOauth2GuestContextMutate(
      {},
      {
        guestToken: guest.guestToken,
      },
    );
  };

  return (
    <button
      type={type}
      className={classname}
      aria-label="프로필"
      onClick={handleClick}
      {...restProps}
    >
      <img src={profileIcon} alt="" />
    </button>
  );
};
