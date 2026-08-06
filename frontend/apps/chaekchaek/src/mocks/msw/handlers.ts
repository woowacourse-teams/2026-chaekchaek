import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('/health', () => {
    return HttpResponse.json({ ok: 'health' }, { status: 200 });
  }),
];

// import { fromOpenApi } from '@mswjs/source/open-api';

// const openapiSpec = {
//   openapi: '3.0.0',
//   info: {
//     version: '1.0.0',
//     title: 'project API docs',
//     description: 'project API docs',
//     license: {
//       name: 'project',
//     },
//   },
//   tags: [
//     {
//       name: 'health',
//       description: 'health check 관련 API',
//     },
//   ],
//   servers: [
//     {
//       url: '/',
//     },
//   ],
//   paths: {
//     '/health': {
//       get: {
//         operationId: 'getHealth',
//         tags: ['health'],
//         description: 'health',
//         responses: {
//           '200': {
//             description: 'health',
//             content: {
//               'application/json': {
//                 schema: {
//                   type: 'object',
//                   properties: {
//                     ok: {
//                       type: 'boolean',
//                     },
//                   },
//                 },
//               },
//             },
//           },
//         },
//       },
//     },
//   },
// };

// export const handlers = await fromOpenApi(JSON.parse(JSON.stringify(openapiSpec)));
