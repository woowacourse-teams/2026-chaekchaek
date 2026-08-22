import { Routes, Route } from 'react-router-dom';

import { routes } from './routes';

function AppRoutes() {
  return (
    <>
      <Routes>
        {routes.map((route) => (
          <Route path={route.path} element={route.element} />
        ))}
      </Routes>
    </>
  );
}

export { AppRoutes };
