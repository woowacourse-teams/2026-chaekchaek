import { getResponseData, getType } from '../generateUtils';

export const component = (name, endPoint) =>
  `${Object.entries(endPoint)
    .map(([method, endPointValue]) => {
      const upperMethod = method.charAt(0).toUpperCase() + method.slice(1);

      const key = name;
      const upperKey = name.charAt(0).toUpperCase() + name.slice(1);

      const obj = endPointValue.parameters?.reduce((acc, parameter) => {
        const type = getType(parameter.schema.type);
        acc[parameter.name] = type;
        return acc;
      }, {});

      const requestData =
        endPointValue.requestBody?.content['application/json']?.schema.properties ||
        endPointValue.requestBody?.content['application/json;charset=UTF-8']?.schema.properties;

      const responseData = endPointValue.responses[200]?.content?.['application/json'].schema;

      const suffixs = {
        get: 'params',
        post: 'command',
        put: 'command',
        delete: 'params',
      };

      const suffix = suffixs[method] || 'params';
      const upperSuffix = suffix.charAt(0).toUpperCase() + suffix.slice(1);

      return `export interface ${upperMethod}${upperKey}${upperSuffix} {
  ${Object.entries(obj || {})
    .filter(([key, value]) => value)
    .map(([key, value]) => `${key}: ${value}`)
    .join(';')}
  ${
    requestData && Object.entries(requestData).length
      ? `;${Object.entries(requestData)
          .map(([key, value]) => `${key}: ${value.type}`)
          .join(';')}`
      : ''
  }
}

export type ${upperMethod}${upperKey} = (${suffix}: ${upperMethod}${upperKey}${upperSuffix}) => Promise<${getResponseData(responseData || {})}>;`;
    })
    .join('')}`;
