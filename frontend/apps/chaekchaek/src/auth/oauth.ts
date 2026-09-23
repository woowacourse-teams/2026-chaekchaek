import { ENV } from '@/configs/env';

type OauthProvider = 'google';

// local: 프론트 로컬 개발 환경
// dev: 프론트 개발 서버
// prod: 프론트 운영 서버
type AppEnv = 'local' | 'development' | 'production';
const clientEnvs = {
  local: 'local',
  development: 'dev',
  production: 'prod',
} as const satisfies Record<AppEnv, string>;

export const getOauthLoginUrl = (provider: OauthProvider) => {
  const url = new URL(`/api/v1/auth/oauth2/${provider}`, ENV.APP_API_URL);

  url.searchParams.set('client', clientEnvs[ENV.APP_ENV]);

  return url.toString();
};
