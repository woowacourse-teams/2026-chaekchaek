import { useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { useLoadData } from '@/services/core/useLoadData';
import { getMembersMe } from '@/services/apis/membersMe/repository';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { ROUTES } from '@/constants/routes';

export const OauthPage = () => {
  const getMembersMeLoadData = useCallback(async () => {
    return await getMembersMe({});
  }, []);
  const {
    status: { data: membersMeData },
  } = useLoadData({
    queryFn: getMembersMeLoadData,
  });

  const { login } = useAuthContext();

  const navigation = useNavigate();

  useEffect(() => {
    if (membersMeData) return login(membersMeData);

    navigation(ROUTES.HOME);
  }, [membersMeData]);

  return null;
};
