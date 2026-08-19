export const component = (name, endPoint) =>
  `import * as fetcher from "./fetcher";
${Object.entries(endPoint)
  .map(([method, endPointValue]) => {
    const upperMethod = method.charAt(0).toUpperCase() + method.slice(1);

    const key = name;
    const upperKey = name.charAt(0).toUpperCase() + name.slice(1);

    const requestParameter = endPointValue.parameters.reduce(
      (acc, parameter) => {
        if (parameter.in === 'query') {
          acc.query[parameter.name] = parameter.schema.type;
        }

        if (parameter.in === 'path') {
          acc.pathParams.push({ name: parameter.name, value: parameter.schema.type });
        }

        return acc;
      },
      { query: {}, pathParams: [] },
    );

    const requestData = endPointValue.requestBody?.content['application/json'].schema.properties;

    return `import {
  map${upperMethod}${upperKey}ModelToRequestDTO,
  map${upperMethod}${upperKey}ResponseDTOToModel,
} from "./mapper";

import type {
  ${upperMethod}${upperKey},
} from "./repository.types";

export const ${method}${upperKey}: ${upperMethod}${upperKey} = async (model) => {
  const { ${Object.entries(requestParameter)
    .map(([parameterIn, parameterValue]) => {
      if (parameterIn === 'query') return `${Object.keys(parameterValue).join(',')}`;
      if (parameterIn === 'pathParams')
        return `${parameterValue
          .map((parameterV) => {
            return `${parameterV.name}`;
          })
          .join(',')}`;
    })
    .filter(Boolean)
    .join(',')} 
    ${requestData && Object.keys(requestData).length ? `, ${Object.keys(data).join(',')}` : ''}
    } = map${upperMethod}${upperKey}ModelToRequestDTO(model);

  const responseDTO = await fetcher.${method}${upperKey}({
  ${Object.entries(requestParameter)
    .map(([parameterIn, parameterValue]) => {
      if (parameterIn === 'query' && Object.keys(parameterValue).length !== 0)
        return `${parameterIn}: {${Object.keys(parameterValue).join(',')}},`;
      if (parameterIn === 'pathParams' && parameterValue.length !== 0)
        return `${parameterIn}: [${parameterValue
          .map((parameterV) => {
            return `{ name: '${parameterV.name}', value: ${parameterV.name} }`;
          })
          .join(',')}],`;
    })
    .join('')}
    ${requestData && Object.keys(requestData).length ? `data: {${Object.keys(data).join(',')}}` : ''}
  });

  return map${upperMethod}${upperKey}ResponseDTOToModel(responseDTO);
};`;
  })
  .join('')}`;
