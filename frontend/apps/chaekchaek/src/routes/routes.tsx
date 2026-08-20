import type { ReactNode } from 'react';

import { ROUTES } from '@/constants/routes';

import { IntroPage } from '@/pages/IntroPage';
import { BooksPage } from '@/pages/BooksPage';
import { BookDetailPage } from '@/pages/BookDetailPage';
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
  {
    path: ROUTES.BOOK_SEARCH,
    element: <BooksPage />,
  },
  {
    path: ROUTES.BOOK_DETAIL + '/:isbn',
    element: <BookDetailPage />,
  },
];

export { routes };
