import { useState, useEffect, useCallback } from 'react';

import type { Options, Status, Result } from './useLoadData.types';
import { RequestAjaxError } from '../http/requestAjaxError';

export const useLoadData = <TData = unknown>({ queryFn }: Options<TData>): Result<TData> => {
  const [status, setStatus] = useState<Status<TData>>({
    status: 'idle',
    data: null,
    error: null,
  });

  const fetchData = useCallback(async (): Promise<TData | void> => {
    setStatus({
      status: 'loading',
      data: null,
      error: null,
    });

    try {
      const data = (await queryFn()) as TData;
      setStatus({
        status: 'success',
        data: data,
        error: null,
      });
      return data;
    } catch (error) {
      setStatus({
        status: 'error',
        data: null,
        error: error instanceof RequestAjaxError ? error?.data : error,
      });
    }
  }, [queryFn]);

  const refetch = useCallback(() => {
    return fetchData();
  }, [fetchData]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  return { status, refetch };
};
