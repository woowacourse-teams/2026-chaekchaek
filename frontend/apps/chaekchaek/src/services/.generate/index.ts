import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'librariesOrder';
const endpoint = endPoint['/api/v1/libraries/order'] || {};

generateApi(name, endpoint);
