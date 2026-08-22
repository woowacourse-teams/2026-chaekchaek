import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'reviewsReviewIdReactions';
const endpoint = endPoint['/api/v1/reviews/{reviewId}/reactions'] || {};

generateApi(name, endpoint);
