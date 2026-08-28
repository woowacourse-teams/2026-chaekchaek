import { Button } from '@chaekchaek/design-system';

import { getOauthLoginUrl } from '@/auth/oauth';

import appleIcon from './assets/apple.svg';
import googleIcon from './assets/google.svg';
import kakaoIcon from './assets/kakao.svg';
import type { SocialLoginButtonProps } from './SocialLoginButton.types';
import styles from './SocialLoginButton.module.css';

const providerDetails = {
  kakao: { label: '카카오로 시작하기', icon: kakaoIcon, link: `#` },
  apple: {
    label: 'Apple로 시작하기',
    icon: appleIcon,
    link: `#`,
  },
  google: {
    label: 'Google로 시작하기',
    icon: googleIcon,
    link: getOauthLoginUrl('google'),
  },
} as const;

export const SocialLoginButton = ({ provider, reverse }: SocialLoginButtonProps) => {
  const { label, icon, link } = providerDetails[provider];

  return (
    <Button
      as="a"
      href={link}
      block
      size="large"
      className={`${styles.root} ${styles[provider]} ${reverse ? styles.reverse : ''}`}
      leading={<img className={styles.icon} src={icon} alt="" />}
    >
      {label}
    </Button>
  );
};
