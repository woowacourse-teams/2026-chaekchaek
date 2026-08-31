export type RequestHeaders = {
  'X-Guest-Token'?: string;
};

export type RequestHeaderContext = {
  guestToken: string | undefined;
};

export const createRequestHeaders = (context?: RequestHeaderContext): Record<string, string> => {
  if (!context?.guestToken) return {};

  return {
    'X-Guest-Token': context.guestToken,
  };
};
