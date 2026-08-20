import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'libraryBookIdRating';
const endpoint = endPoint['/api/v1/library/{bookId}/rating'] || {};

generateApi(name, endpoint);
