import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'librariesLibraryId';
const endpoint = endPoint['/api/v1/libraries/{libraryId}'] || {};

generateApi(name, endpoint);
