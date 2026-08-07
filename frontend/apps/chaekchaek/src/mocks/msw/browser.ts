import { setupWorker } from 'msw/browser';

export async function enableMocking() {
  const { handlers } = await import('./handlers');

  const worker = setupWorker(...handlers);

  return worker.start();
}
