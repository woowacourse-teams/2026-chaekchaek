import { useContext } from 'react';

import { authContext } from './AuthContext';

const useAuthContext = () => {
  const ctx = useContext(authContext);

  if (!ctx) throw new Error('no AuthContext');

  return ctx;
};

export { useAuthContext };
