import { useCallback, useEffect } from 'react';

import { ENV } from '@/configs/env';

import { useLoadData } from '@/services/core/useLoadData';
import { getMembersMe } from '@/services/apis/membersMe/repository';

export const OauthPage = () => {
  const getMembersMeLoadData = useCallback(async () => {
    return await getMembersMe({});
  }, []);
  const {
    status: { data: membersMeData },
  } = useLoadData({
    queryFn: getMembersMeLoadData,
  });

  return null;
};
