import { useState, useEffect, useCallback, useRef } from 'react';

import type { Options, Status, Result } from './useInfiniteLoadData.types';
import { RequestAjaxError } from '../http/requestAjaxError';

export const useInfiniteLoadData = <TData = unknown>({
  queryFn,
  initialPageParam,
  getNextPageParam,
}: Options<TData>): Result<TData> => {
  const [status, setStatus] = useState<Status<TData>>({
    status: 'idle',
    data: null,
    error: null,
  });

  const lastDataRef = useRef<TData | null>(null);

  const fetchData = useCallback(
    async ({ pageParam }: { pageParam: number }): Promise<TData | void> => {
      setStatus((prev) => ({
        ...prev,
        status: 'loading',
      }));

      try {
        const data = (await queryFn({ pageParam: pageParam })) as TData;

        setStatus({
          status: 'success',
          data: {
            pages: [data],
            pageParams: [pageParam],
          },
          error: null,
        });

        lastDataRef.current = data;

        return data;
      } catch (error) {
        setStatus({
          status: 'error',
          data: null,
          error: error instanceof RequestAjaxError ? error?.data : error,
        });
      }
    },
    [queryFn, lastDataRef],
  );

  const fetchNextData = useCallback(
    async ({ pageParam }: { pageParam: number }): Promise<TData | void> => {
      setStatus((prev) => ({
        ...prev,
        status: 'loading',
      }));

      try {
        const data = (await queryFn({ pageParam: pageParam })) as TData;

        setStatus((prev) => ({
          status: 'success',
          data: {
            pages: [...((prev.data?.pages ?? []) as TData[]), data],
            pageParams: [...((prev.data?.pageParams ?? []) as number[]), pageParam],
          },
          error: null,
        }));

        lastDataRef.current = data;

        return data;
      } catch (error) {
        setStatus({
          status: 'error',
          data: null,
          error: error instanceof RequestAjaxError ? error?.data : error,
        });
      }
    },
    [queryFn, lastDataRef],
  );

  const refetch = useCallback(() => {
    return fetchData({
      pageParam: initialPageParam,
    });
  }, [fetchData]);

  const fetchNextPage = useCallback(async () => {
    return fetchNextData({
      pageParam: getNextPageParam(lastDataRef.current),
    });
  }, [fetchData, getNextPageParam]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  return { status, refetch, fetchNextPage };
};
