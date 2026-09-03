import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'authOauth2GuestContext';
const endpoint = endPoint['/api/v1/auth/oauth2/guest-context'] || {};

generateApi(name, endpoint);
