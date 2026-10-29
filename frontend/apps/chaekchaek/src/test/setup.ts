import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';

import { afterAll, afterEach, beforeAll } from 'vitest';

import { server } from '@/mocks/msw/server';

configure({
  getElementError: function getElementError(message) {
    const error = new Error(message?.trim().split('\n')[0] ?? '');
    error.name = 'TestingLibraryElementError';
    Error.captureStackTrace(error, getElementError);
    return error;
  },
});

beforeAll(() => {
  server.listen({
    onUnhandledRequest: 'error',
  });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});
