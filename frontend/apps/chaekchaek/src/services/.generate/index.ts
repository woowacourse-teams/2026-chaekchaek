import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'librariesLibraryIdBooksOrder';
const endpoint = endPoint['/api/v1/libraries/{libraryId}/books/order'] || {};

generateApi(name, endpoint);
