import type { ReactNode } from 'react';

interface Route {
  path: string;
  element: ReactNode;
}

const DummyPage = () => {
  return 'dummy page';
};

const routes: Route[] = [
  {
    path: '/',
    element: <DummyPage />,
  },
];

export { routes };
