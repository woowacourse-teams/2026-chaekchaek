import type { ReactNode } from 'react';

import { ROUTES } from '@/constants/routes';

import { BooksPage } from '@/pages/BooksPage';

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
    path: ROUTES.BOOK_SEARCH,
    element: <BooksPage />,
  },
];

export { routes };
