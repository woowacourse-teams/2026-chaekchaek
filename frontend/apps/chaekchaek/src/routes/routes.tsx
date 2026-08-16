import type { ReactNode } from 'react';

import { ROUTES } from '@/constants/routes';

interface Route {
  path: string;
  element: ReactNode;
}

const DummyPage = () => {
  return 'dummy page';
};

const routes: Route[] = [
  {
    path: ROUTES.HOME,
    element: <DummyPage />,
  },
];

export { routes };
