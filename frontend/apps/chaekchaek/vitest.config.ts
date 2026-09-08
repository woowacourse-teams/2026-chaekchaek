import { fileURLToPath } from 'node:url';

import { defineConfig } from 'vitest/config';

import svgr from 'vite-plugin-svgr';

import dotenv from 'dotenv';

dotenv.config({
  path: '.env.test',
});

const srcPath = fileURLToPath(new URL('./src', import.meta.url));

export default defineConfig({
  resolve: {
    alias: {
      '@': srcPath,
    },
  },

  test: {
    environment: 'jsdom',
    env: process.env,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    exclude: ['node_modules', 'dist'],
    globals: false,
    restoreMocks: true,
    clearMocks: true,
    mockReset: true,
    typecheck: {
      enabled: true,
      tsconfig: './tsconfig.test.json',
    },
  },

  plugins: [
    svgr({
      include: '**/*.svg?component',
    }),
  ],

  define: {
    __DEV__: JSON.stringify(true),
  },
});
