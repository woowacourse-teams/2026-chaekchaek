import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'membersMeNickname';
const endpoint = endPoint['/api/v1/members/me/nickname'] || {};

generateApi(name, endpoint);
