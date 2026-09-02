import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'authGuestTokenRefreshs';
const endpoint = endPoint['/api/v1/auth/guest-token/refresh'] || {};

generateApi(name, endpoint);
