import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';
import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';
import { playwright } from '@vitest/browser-playwright';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(process.cwd(), 'src')
    }
  },
  test: {
    projects: [
      {
        extends: true,
        resolve: {
          conditions: ['node'],
        },
        test: {
          name: 'unit',
          environment: 'jsdom',
          globals: true,
          setupFiles: ['./src/test/setup.ts'],
          css: true,
          include: ['src/**/*.test.{ts,tsx}'],
          exclude: ['src/test/arch/**'],
          server: {
            deps: {
              inline: [/@telegram-tools/]
            }
          },
        },
      },
      {
        extends: true,
        plugins: [
          storybookTest({
            configDir: resolve(process.cwd(), '.storybook')
          })
        ],
        test: {
          name: 'storybook',
          browser: {
            enabled: true,
            headless: true,
            provider: playwright({}),
            instances: [{
              browser: 'chromium'
            }]
          },
          setupFiles: ['.storybook/vitest.setup.ts']
        }
      },
      {
        extends: true,
        test: {
          name: 'arch',
          environment: 'node',
          include: ['src/test/arch/**/*.test.ts'],
        },
      }
    ]
  }
});
