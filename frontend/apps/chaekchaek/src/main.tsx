import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { AppProviders } from '@/providers/AppProvider';
import { enableMocking } from '@/mocks/msw/browser';

import { initializeGA } from '@/analytics/ga';
import { initializeErrorTracking } from '@/monitoring';

import { App } from '@/App';

const container = document.getElementById('root');

if (!container) throw new Error('root 요소를 찾을 수 없습니다.');

if (__DEV__) {
  await enableMocking();
}

if (!__DEV__) {
  initializeGA();
  initializeErrorTracking();
}

const root = createRoot(container);
root.render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
);
