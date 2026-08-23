import type { ReactNode } from 'react';

import { BrowserRouter as Router } from 'react-router-dom';

import { AuthProvider } from '@/contexts/AuthContext/AuthProvider';

type Props = {
  children: ReactNode;
};

const AppProviders = ({ children }: Props) => {
  return (
    <Router>
      <AuthProvider>{children}</AuthProvider>
    </Router>
  );
};

export { AppProviders };
