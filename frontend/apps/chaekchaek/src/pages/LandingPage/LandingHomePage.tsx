import { useEffect } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';
import { ROUTES } from '@/constants/routes';

const LandingHomePage = () => {
  const navigate = useNavigate();

  const { isAuthenticated } = useAuthContext();

  useEffect(() => {
    if (isAuthenticated) {
      navigate(ROUTES.INTRO, {
        replace: true,
      });
    }
  }, [navigate, isAuthenticated]);

  if (isAuthenticated) return null;

  return <Navigate to={ROUTES.LANDING} replace />;
};

export { LandingHomePage };
