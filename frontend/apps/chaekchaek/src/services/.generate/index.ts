import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'membersMeRatingsComparison';
const endpoint = endPoint['/api/v1/members/me/ratings/comparison'] || {};

generateApi(name, endpoint);
