import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'libraryBulkStatus';
const endpoint = endPoint['/api/v1/library/bulk-status'] || {};

generateApi(name, endpoint);
