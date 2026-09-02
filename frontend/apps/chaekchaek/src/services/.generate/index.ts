import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'membersMemberIdLibrary';
const endpoint = endPoint['/api/v1/members/{memberId}/library'] || {};

generateApi(name, endpoint);
