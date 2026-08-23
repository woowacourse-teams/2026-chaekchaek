import { Button } from '@chaekchaek/design-system';

import appleIcon from './assets/apple.svg';
import googleIcon from './assets/google.svg';
import kakaoIcon from './assets/kakao.svg';
import type { SocialLoginButtonProps } from './SocialLoginButton.types';
import styles from './SocialLoginButton.module.css';

const providerDetails = {
  kakao: { label: '카카오로 시작하기', icon: kakaoIcon },
  apple: { label: 'Apple로 시작하기', icon: appleIcon },
  google: { label: 'Google로 시작하기', icon: googleIcon },
} as const;

export const SocialLoginButton = ({ provider }: SocialLoginButtonProps) => {
  const { label, icon } = providerDetails[provider];

  return (
    <Button
      as="button"
      type="button"
      block
      size="large"
      className={`${styles.root} ${styles[provider]}`}
      leading={<img className={styles.icon} src={icon} alt="" />}
    >
      {label}
    </Button>
  );
};
