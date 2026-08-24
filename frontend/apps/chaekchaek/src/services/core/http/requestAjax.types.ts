type Method = 'get' | 'post' | 'put' | 'patch' | 'delete';

export type PathParam = {
  name: string;
  value: string | number;
};

export type Configs = {
  method?: Method | undefined;
  url?: string | undefined;
  pathParams?: PathParam[] | undefined;
  query?: Record<string, unknown> | undefined;
  data?: Record<string, unknown> | undefined;
  headers?: Record<string, unknown> | undefined;
};

export type Policy = {
  redirectOnReissueFailure?: boolean;
};

export type RequestFetchResponse = {
  data: any;
  status: number;
  headers?: Record<string, unknown> | undefined;
  config?: Configs | undefined;
};
