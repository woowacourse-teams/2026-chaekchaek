import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'homePopularBooks';
const endpoint = endPoint['/api/v1/home/popular-books'] || {};

generateApi(name, endpoint);
