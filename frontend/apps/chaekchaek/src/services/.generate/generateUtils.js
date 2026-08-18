export const getResponseData = (responseData) => {
  if (responseData.type === 'object')
    return `{
      ${Object.entries(responseData.properties)
        .map(([key, value]) => {
          return `${key}: ${getResponseData(value)}`;
        })
        .join(';')}
          }`;

  if (responseData.type === 'array') return `${getResponseData(responseData.items)}[]`;

  return `${getType(responseData.type)}`;
};

export const getType = (type) => {
  return type === 'integer' ? 'number' : type;
};
