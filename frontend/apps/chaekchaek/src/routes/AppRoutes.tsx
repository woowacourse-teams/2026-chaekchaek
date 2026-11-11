import { Routes, Route } from 'react-router-dom';

import { standaloneRoutes, mainAppRoutes } from './routes';
import { Suspense } from 'react';
import { MainApp } from '@/mainApp';

function AppRoutes() {
  return (
    <Suspense fallback={null}>
      <Routes>
        {standaloneRoutes.map((route) => (
          <Route path={route.path} element={route.element} />
        ))}

        <Route element={<MainApp />}>
          {mainAppRoutes.map((route) => (
            <Route path={route.path} element={route.element} />
          ))}
        </Route>
      </Routes>
    </Suspense>
  );
}

export { AppRoutes };
