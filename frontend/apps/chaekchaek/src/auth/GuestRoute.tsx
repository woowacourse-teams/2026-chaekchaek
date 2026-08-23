import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';
import { ROUTES } from '@/constants/routes';

type Props = {
  children: ReactNode;
};

const GuestRoute = ({ children }: Props) => {
  const navigate = useNavigate();

  const { isAuthenticated } = useAuthContext();

  useEffect(() => {
    if (isAuthenticated) {
      navigate(ROUTES.HOME, {
        replace: true,
      });
    }
  }, [navigate, isAuthenticated]);

  if (isAuthenticated) return null;

  return children;
};

export { GuestRoute };
