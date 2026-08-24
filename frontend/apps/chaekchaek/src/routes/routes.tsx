import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';

import { ROUTES } from '@/constants/routes';

import { GuestRoute } from '@/auth/GuestRoute';
import { ProtectedRoute } from '@/auth/ProtectedRoute';

import { OauthPage } from '@/pages/OauthPage';
import { LoginPage } from '@/pages/LoginPage';

import { IntroPage } from '@/pages/IntroPage';
import { BooksPage } from '@/pages/BooksPage';
import { BookDetailPage } from '@/pages/BookDetailPage';
import { LibraryPage } from '@/pages/LibraryPage';

interface Route {
  path: string;
  element: ReactNode;
}

const DummyPage = () => {
  return 'dummy page';
};

const routes: Route[] = [
  {
    path: ROUTES.LOGIN,
    element: (
      <GuestRoute>
        <LoginPage />
      </GuestRoute>
    ),
  },
  {
    path: ROUTES.OAUTH_CALLBACK,
    element: <OauthPage />,
  },
  {
    path: ROUTES.HOME,
    element: <Navigate to={ROUTES.INTRO} replace />,
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
  {
    path: ROUTES.LIBRARY,
    element: (
      <ProtectedRoute>
        <LibraryPage />
      </ProtectedRoute>
    ),
  },
];

export { routes };
