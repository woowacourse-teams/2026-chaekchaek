// import { http, HttpResponse } from 'msw';

// export const handlers = [
//   http.get('/health', () => {
//     return HttpResponse.json({ ok: 'health' }, { status: 200 });
//   }),
// ];

import { ENV } from '@/configs/env';

import { fromOpenApi } from '@mswjs/source/open-api';

import openapiSpec from '@/services/open-api-spec/openapi3.json';

const apis = JSON.parse(JSON.stringify(openapiSpec));

apis.servers[0].url = ENV.APP_API_URL;

export const handlers = await fromOpenApi(JSON.parse(JSON.stringify(apis)));
