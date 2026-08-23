export type SocialLoginProvider = 'kakao' | 'apple' | 'google';
export type SocialLoginButtonProps = {
  provider: SocialLoginProvider;
  reverse?: boolean;
};
