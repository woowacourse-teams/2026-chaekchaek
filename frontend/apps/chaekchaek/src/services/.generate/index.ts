import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'libraryBulkDelete';
const endpoint = endPoint['/api/v1/library/bulk-delete'] || {};

generateApi(name, endpoint);
