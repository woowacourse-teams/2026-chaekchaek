import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'booksByIsbnIsbn13Reviews';
const endpoint = endPoint['/api/v1/books/by-isbn/{isbn13}/reviews'] || {};

generateApi(name, endpoint);
