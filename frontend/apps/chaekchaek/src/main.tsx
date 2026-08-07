import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { enableMocking } from './mocks/msw/browser';

import { App } from './App';

const container = document.getElementById('root');

if (!container) throw new Error('root 요소를 찾을 수 없습니다.');

if (__DEV__) {
  await enableMocking();
}

const root = createRoot(container);
root.render(
  <StrictMode>
    <App />
  </StrictMode>,
);
