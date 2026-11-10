import openApiSpec from '@/services/open-api-spec/openapi3.json';

import generateApi from './generateApi';

const endPoint = openApiSpec?.paths;

const name = 'pmfSurveyResponses';
const endpoint = endPoint['/api/v1/pmf-survey/responses'] || {};

generateApi(name, endpoint);
