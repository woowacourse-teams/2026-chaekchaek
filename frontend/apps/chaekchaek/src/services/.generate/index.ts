import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'repliesReplyId';
const endpoint = endPoint['/api/v1/replies/{replyId}'] || {};

generateApi(name, endpoint);
