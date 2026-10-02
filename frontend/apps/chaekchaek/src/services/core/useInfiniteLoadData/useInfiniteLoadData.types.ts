export type StatusType = 'idle' | 'loading' | 'success' | 'error';

export type Options<TData = unknown> = {
  queryFn: ({ pageParam }: { pageParam: number }) => Promise<TData>;
  initialPageParam: number;
  getNextPageParam: (lastData: TData | null) => number;
};

type InfiniteData<TData> = {
  pages: TData[];
  pageParams: number[];
};

type IdleStatus = {
  status: 'idle';
  data: null;
  error: null;
};

type SuccessStatus<TData> = {
  status: 'success';
  data: InfiniteData<TData>;
  error: null;
};

type ErrorStatus = {
  status: 'error';
  data: null;
  error: unknown;
};

type LoadingStatus<TData> = {
  status: 'loading';
  data: InfiniteData<TData> | null;
  error: unknown | null;
};

export type Status<TData = unknown> =
  IdleStatus | SuccessStatus<TData> | ErrorStatus | LoadingStatus<TData>;

export type Result<TData = unknown> = {
  status: Status<TData>;
  refetch: () => Promise<TData | void>;
  fetchNextPage: () => Promise<TData | void>;
};
