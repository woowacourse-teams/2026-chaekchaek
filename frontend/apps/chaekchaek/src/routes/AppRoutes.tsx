import { Routes, Route } from 'react-router-dom';

import { routes } from './routes';
import { Suspense } from 'react';

function AppRoutes() {
  return (
    <Suspense fallback={null}>
      <Routes>
        {routes.map((route) => (
          <Route path={route.path} element={route.element} />
        ))}
      </Routes>
    </Suspense>
  );
}

export { AppRoutes };
