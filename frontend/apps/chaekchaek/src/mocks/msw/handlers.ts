// import { http, HttpResponse } from 'msw';

// export const handlers = [
//   http.get('/health', () => {
//     return HttpResponse.json({ ok: 'health' }, { status: 200 });
//   }),
// ];

import { fromOpenApi } from '@mswjs/source/open-api';

import openapiSpec from '@/services/open-api-spec/openapi3.json';

export const handlers = await fromOpenApi(JSON.parse(JSON.stringify(openapiSpec)));
