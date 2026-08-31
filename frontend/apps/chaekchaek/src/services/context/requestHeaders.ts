export type RequestHeaders = {
  'X-Guest-Token'?: string;
};

export type RepositoryRequestHeaderContext = {
  guestToken: string | undefined;
};

export const createRepositoryRequestHeaders = (
  context?: RepositoryRequestHeaderContext,
): Record<string, string> => {
  if (!context?.guestToken) return {};

  return {
    'X-Guest-Token': context.guestToken,
  };
};
type FetcherHeaders = {
  'X-Guest-Token': string | undefined;
};

export const createFetcherRequestHeaders = ({
  'X-Guest-Token': guestToken,
}: FetcherHeaders): Record<string, string> => {
  if (!guestToken) return {};

  return {
    'X-Guest-Token': guestToken,
  };
};
