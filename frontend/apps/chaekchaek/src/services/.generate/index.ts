import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'booksIsbn';
const endpoint = endPoint['/api/v2/books/by-isbn/{isbn13}'] || {};

generateApi(name, endpoint);
