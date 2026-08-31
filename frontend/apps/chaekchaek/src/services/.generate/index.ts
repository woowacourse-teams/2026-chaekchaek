import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'authGuestToken';
const endpoint = endPoint['/api/v1/auth/guest-token'] || {};

generateApi(name, endpoint);
