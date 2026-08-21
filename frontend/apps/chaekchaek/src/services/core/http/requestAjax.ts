import { ENV } from '@/configs/env';

import { RequestAjaxError, RequestNetworkError } from './requestAjaxError';

import type { Configs, RequestFetchResponse } from './requestAjax.types';

export const requestAjax = async (url: string, config?: Configs): Promise<RequestFetchResponse> => {
  const { method = 'get', url: configUrl, pathParams, query, data, headers } = config || {};

  let finalUrl = `${ENV.APP_API_URL || ''}${configUrl || url}`;

  if (pathParams) {
    const paramsString = pathParams.map(({ value }) => value).join('/');
    finalUrl += `/${paramsString}`;
  }

  if (query) {
    const querystring = new URLSearchParams(query as Record<string, string>).toString();
    finalUrl += `?${querystring}`;
  }

  const customHeaders = {
    'Content-Type': 'application/json',
    ...headers,
  };

  let res;
  try {
    res = await fetch(finalUrl, {
      method: method.toUpperCase(),
      ...(!!Object.values(customHeaders).filter(Boolean).length && {
        headers: {
          ...(customHeaders as Record<string, string>),
        },
      }),
      credentials: 'include',
      ...(data && {
        body: data instanceof FormData ? data : JSON.stringify(data),
      }),
    });
  } catch (error) {
    const response = {
      data: error,
      headers: customHeaders,
      config,
    };
    throw new RequestNetworkError(response);
  }

  let responseData = await res.text();
  try {
    responseData = JSON.parse(responseData);
  } catch (e) {
    console.error(e);
  }

  const response = {
    data: responseData,
    status: res.status,
    headers: customHeaders,
    config,
  };
  if (res.ok) {
    return response;
  } else {
    throw new RequestAjaxError(response);
  }
};

export const create = () => {
  return async (url: string, config?: Configs) => {
    try {
      return await requestAjax(url, config);
    } catch (error) {
      if (error instanceof RequestAjaxError && error.status === 401) {
        try {
          await requestAjax(`/api/v1/auth/reissue`, { method: 'post' });
        } catch (error) {
          window.location.href = `/login`;
          throw error;
        }

        return await requestAjax(url, config);
      }
      throw error;
    }
  };
};
