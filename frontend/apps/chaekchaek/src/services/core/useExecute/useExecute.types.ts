export type StatusType = "idle" | "loading" | "success" | "error";

export type Options<TArgs extends unknown[], TData = unknown> = {
  executeFn: (...rest: TArgs) => Promise<TData>;
  onSuccess?: (data: TData) => void;
  onError?: (error: unknown) => void;
};

type IdleStatus = {
  status: "idle";
  data: null;
  error: null;
};

type SuccessStatus<TData> = {
  status: "success";
  data: TData;
  error: null;
};

type ErrorStatus = {
  status: "error";
  data: null;
  error: unknown;
};

type LoadingStatus = {
  status: "loading";
  data: null;
  error: null;
};

export type Status<TData = unknown> =
  | IdleStatus
  | SuccessStatus<TData>
  | ErrorStatus
  | LoadingStatus;

export type Result<TArgs extends unknown[], TData = unknown> = {
  status: Status<TData>;
  mutate: (...rest: TArgs) => Promise<TData | void>;
};
