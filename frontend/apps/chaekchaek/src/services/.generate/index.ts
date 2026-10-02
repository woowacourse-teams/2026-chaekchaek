import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'feedReviews';
const endpoint = endPoint['/api/v1/feed/reviews'] || {};

generateApi(name, endpoint);
