import { getType } from '../generateUtils';

export const component = (name, endPoint) => `import { requestAjax } from '@/services/core/http';
${Object.entries(endPoint)
  .map(([method, endPointValue]) => {
    const upperMethod = method.charAt(0).toUpperCase() + method.slice(1);

    const key = name;
    const upperKey = name.charAt(0).toUpperCase() + name.slice(1);

    const requestParameter = endPointValue.parameters.reduce(
      (acc, parameter) => {
        const type = getType(parameter.schema.type);

        if (parameter.in === 'path') {
          acc.pathParams[parameter.name] = type;
        }
        if (parameter.in === 'query') {
          acc.query[parameter.name] = type;
        }
        return acc;
      },
      { pathParams: {}, query: {} },
    );

    const requestData = endPointValue.requestBody?.content['application/json'].schema.properties;

    return `
import type {
  ${upperMethod}${upperKey}RequestDto,
  ${upperMethod}${upperKey}ResponseDto,
} from "./dto";

export const ${method}${upperKey} = async ({ 
  ${Object.entries(requestParameter)
    .filter(([parameterIn, parameterValue]) => {
      return Object.keys(parameterValue).length !== 0;
    })
    .map(([parameterIn, parameterValue]) => {
      if (parameterIn === 'pathParams')
        return `${parameterIn}: [${Object.entries(parameterValue)
          .map(([key, value]) => `{value: ${key}}`)
          .join(',')}]`;

      if (parameterIn === 'query')
        return `${parameterIn}: {${Object.entries(parameterValue)
          .map(([key, value]) => `${key}`)
          .join(',')}}`;

      return false;
    })
    .join(',')}
  ${
    requestData
      ? `, data: {${Object.entries(requestData)
          .map(([key, value]) => {
            return `${key}`;
          })
          .join(',')}
    }`
      : ''
  }
}: ${upperMethod}${upperKey}RequestDto): Promise<${upperMethod}${upperKey}ResponseDto> => {
  const response = await requestAjax('/api/v1/${name}', {
    method: '${method}',
   ${Object.entries(requestParameter)
     .filter(([parameterIn, parameterValue]) => {
       return Object.keys(parameterValue).length !== 0;
     })
     .map(([parameterIn, parameterValue]) => {
       if (parameterIn === 'pathParams')
         return `${parameterIn}: [${Object.entries(parameterValue)
           .map(([key, value]) => `{ name: '${key}', value: ${key} }`)
           .join(',')}]`;

       if (parameterIn === 'query')
         return `${parameterIn}: {${Object.keys(parameterValue).join(',')}}`;
     })
     .join(',')}
  ${
    requestData
      ? `,
    data: {${Object.entries(requestData)
      .map(([key, value]) => {
        return `${key}`;
      })
      .join(',')}
    }`
      : ''
  }
  });

  return response.data;
};
`;
  })
  .join('')}`;
