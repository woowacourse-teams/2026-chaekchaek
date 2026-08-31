import type { RequestContext } from './requestContext';

export type RequestHeaders = {
  'X-Guest-Token'?: string;
};

export const createRequestHeaders = (context?: RequestContext): Record<string, string> => {
  if (!context) return {};

  return {
    'X-Guest-Token': context.guestToken,
  };
};
