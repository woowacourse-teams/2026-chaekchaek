import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'reviewsReviewIdReplies';
const endpoint = endPoint['/api/v1/reviews/{reviewId}/replies'] || {};

generateApi(name, endpoint);
