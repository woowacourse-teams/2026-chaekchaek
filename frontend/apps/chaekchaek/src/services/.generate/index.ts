import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'librariesLibraryIdBooksBookId';
const endpoint = endPoint['/api/v1/libraries/{libraryId}/books/{bookId}'] || {};

generateApi(name, endpoint);
