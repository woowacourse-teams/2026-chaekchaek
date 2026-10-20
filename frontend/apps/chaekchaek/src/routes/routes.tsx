import { lazy } from 'react';
import type { ReactElement } from 'react';
import { Navigate } from 'react-router-dom';

import { ROUTES } from '@/constants/routes';

import { IntroPage } from '@/pages/IntroPage';

const GuestRoute = lazy(
  async () =>
    await import('@/auth/GuestRoute').then((module) => ({
      default: module.GuestRoute,
    })),
);
const ProtectedRoute = lazy(
  async () =>
    await import('@/auth/ProtectedRoute').then((module) => ({
      default: module.ProtectedRoute,
    })),
);

const OauthPage = lazy(
  async () =>
    await import('@/pages/OauthPage').then((module) => ({
      default: module.OauthPage,
    })),
);

const LoginPage = lazy(
  async () =>
    await import('@/pages/LoginPage').then((module) => ({
      default: module.LoginPage,
    })),
);

const DownloadPage = lazy(
  async () =>
    await import('@/pages/DownloadPage').then((module) => ({
      default: module.DownloadPage,
    })),
);

const LandingPage = lazy(
  async () =>
    await import('@/pages/LandingPage').then((module) => ({
      default: module.LandingPage,
    })),
);

const LandingHomePage = lazy(
  async () =>
    await import('@/pages/LandingPage').then((module) => ({
      default: module.LandingHomePage,
    })),
);

const BooksPage = lazy(
  async () =>
    await import('@/pages/BooksPage').then((module) => ({
      default: module.BooksPage,
    })),
);
const BookDetailPage = lazy(
  async () =>
    await import('@/pages/BookDetailPage').then((module) => ({
      default: module.BookDetailPage,
    })),
);
const LibraryPage = lazy(
  async () =>
    await import('@/pages/LibraryPage').then((module) => ({
      default: module.LibraryPage,
    })),
);
const MemberLibraryPage = lazy(
  async () =>
    await import('@/pages/MemberLibraryPage').then((module) => ({
      default: module.MemberLibraryPage,
    })),
);
const BookFeedPage = lazy(
  async () =>
    await import('@/pages/BookFeedPage').then((module) => ({
      default: module.BookFeedPage,
    })),
);

interface Route {
  path: string;
  element: ReactElement;
}

const DummyPage = () => {
  return 'dummy page';
};

const routes: Route[] = [
  {
    path: ROUTES.QR,
    element: <Navigate to={ROUTES.HOME} replace />,
  },
  {
    path: ROUTES.DOWNLOAD,
    element: <DownloadPage />,
  },
  {
    path: ROUTES.LOGIN,
    element: (
      <GuestRoute>
        <LoginPage />
      </GuestRoute>
    ),
  },
  {
    path: ROUTES.LANDING,
    element: <LandingPage />,
  },
  {
    path: ROUTES.OAUTH_CALLBACK,
    element: <OauthPage />,
  },
  {
    path: ROUTES.HOME,
    element: <LandingHomePage />,
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
    path: ROUTES.LEGACY_LIBRARY,
    element: (
      <ProtectedRoute>
        <LibraryPage />
      </ProtectedRoute>
    ),
  },
  {
    path: ROUTES.LEGACY_MEMBER_LIBRARY,
    element: <MemberLibraryPage />,
  },
  {
    path: ROUTES.FEED,
    element: <BookFeedPage />,
  },
];

export { routes };
