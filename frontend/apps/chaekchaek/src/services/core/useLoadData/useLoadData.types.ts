export type StatusType = 'idle' | 'loading' | 'success' | 'error';

export type Options<TData = unknown> = {
  queryFn: () => Promise<TData>;
};

type IdleStatus = {
  status: 'idle';
  data: null;
  error: null;
};

type SuccessStatus<TData> = {
  status: 'success';
  data: TData;
  error: null;
};

type ErrorStatus = {
  status: 'error';
  data: null;
  error: unknown;
};

type LoadingStatus = {
  status: 'loading';
  data: null;
  error: null;
};

export type Status<TData = unknown> =
  IdleStatus | SuccessStatus<TData> | ErrorStatus | LoadingStatus;

export type Result<TData = unknown> = {
  status: Status<TData>;
  refetch: () => Promise<TData | void>;
};
