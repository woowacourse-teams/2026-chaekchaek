import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'reviewsReviewId';
const endpoint = endPoint['/api/v1/reviews/{reviewId}'] || {};

generateApi(name, endpoint);
