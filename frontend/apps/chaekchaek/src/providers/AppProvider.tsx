import type { ReactNode } from 'react';

import { BrowserRouter as Router } from 'react-router-dom';

type Props = {
  children: ReactNode;
};

const AppProviders = ({ children }: Props) => {
  return <Router>{children}</Router>;
};

export { AppProviders };
