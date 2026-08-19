import { getResponseData, getType } from '../generateUtils';

export const component = (
  name,
  endPoint,
) => `import type { ResponseDto } from "@/services/apis/api.types";

${Object.entries(endPoint)
  .map(([method, endPointValue]) => {
    const upperMethod = method.charAt(0).toUpperCase() + method.slice(1);

    const key = name;
    const upperKey = name.charAt(0).toUpperCase() + name.slice(1);

    const requestParameter = endPointValue.parameters?.reduce(
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

    const requestData =
      endPointValue.requestBody?.content['application/json']?.schema.properties ||
      endPointValue.requestBody?.content['application/json;charset=UTF-8']?.schema.properties;

    const responseData = endPointValue.responses[200]?.content['application/json']?.schema;

    return `export interface ${upperMethod}${upperKey}RequestDto {
${Object.entries(requestParameter || {})
  .map(([parameterIn, parameterValue]) => {
    if (parameterIn === 'pathParams' && Object.keys(parameterValue).length !== 0)
      return `
  ${parameterIn}: [${Object.entries(parameterValue)
    .map(([key, value]) => `{name: '${key}'; value: ${value}}`)
    .join(',')}];`;

    if (parameterIn === 'query' && Object.keys(parameterValue).length !== 0)
      return `
  ${parameterIn}: {${Object.entries(parameterValue)
    .map(([key, value]) => `${key}: ${value}`)
    .join(';')};}`;
  })
  .join('')}
  ${
    requestData && Object.keys(requestData).length
      ? `data: {${Object.entries(requestData).map(([key, value]) => {
          return `${key}: ${value.type}`;
        })}
    }`
      : ''
  }
}

export type ${upperMethod}${upperKey}ResponseDto = ResponseDto<
  ${getResponseData(responseData || {})}
>;
`;
  })
  .join('')}`;
