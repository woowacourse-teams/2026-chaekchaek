import type { ReactNode } from 'react';

import { ROUTES } from '@/constants/routes';

import { IntroPage } from '@/pages/IntroPage';

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
  {
    path: ROUTES.INTRO,
    element: <IntroPage />,
  },
];

export { routes };
