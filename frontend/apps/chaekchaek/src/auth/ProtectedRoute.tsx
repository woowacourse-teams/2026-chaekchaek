import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';
import { ROUTES } from '@/constants/routes';

type Props = {
  children: ReactNode;
};

const ProtectedRoute = ({ children }: Props) => {
  const navigate = useNavigate();
  const location = useLocation();

  const { isAuthenticated } = useAuthContext();

  useEffect(() => {
    // if (!isAuthenticated) {
    //   navigate(ROUTES.LOGIN, {
    //     replace: true,
    //     state: { from: location },
    //   });
    // }
  }, [navigate, location, isAuthenticated]);

  if (!isAuthenticated) return null;

  return children;
};

export { ProtectedRoute };
