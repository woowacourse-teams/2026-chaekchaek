import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { worker } from './mocks/msw/browser';

import { App } from './App';

const container = document.getElementById('root');

if (!container) throw new Error('root 요소를 찾을 수 없습니다.');

await worker.start({
  serviceWorker: {
    url: `/mockServiceWorker.js`,
  },
});

const root = createRoot(container);
root.render(
  <StrictMode>
    <App />
  </StrictMode>,
);
