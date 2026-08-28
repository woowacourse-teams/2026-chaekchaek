import { ENV } from '@/configs/env';

type OauthProvider = 'google';

// local: 프론트 로컬 개발 환경
// dev: 프론트 개발 서버
const CLIENT_ENV = __DEV__ ? 'local' : 'dev';

export const getOauthLoginUrl = (provider: OauthProvider) => {
  const url = new URL(`/api/v1/auth/oauth2/${provider}`, ENV.APP_API_URL);

  url.searchParams.set('client', CLIENT_ENV);

  return url.toString();
};
