import { Button } from '@chaekchaek/design-system';

import { ENV } from '@/configs/env';

import appleIcon from './assets/apple.svg';
import googleIcon from './assets/google.svg';
import kakaoIcon from './assets/kakao.svg';
import type { SocialLoginButtonProps } from './SocialLoginButton.types';
import styles from './SocialLoginButton.module.css';

// local: 프론트 로컬 개발 환경
// dev: 프론트 개발 서버
const clientEnv = `client=${__DEV__ ? 'local' : 'dev'}`;

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
    link: `${ENV.APP_API_URL}/api/v1/auth/oauth2/google?${clientEnv}`,
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
