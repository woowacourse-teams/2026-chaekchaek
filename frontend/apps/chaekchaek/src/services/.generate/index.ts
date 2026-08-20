import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'library';
const endpoint = endPoint['/api/v1/library'] || {};

generateApi(name, endpoint);
