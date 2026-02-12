import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { RenderOptions } from '@testing-library/react';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import type { ReactElement, ReactNode } from 'react';
import { initReactI18next } from 'react-i18next';
import { MemoryRouter } from 'react-router';
import en from '@/shared/i18n/locales/en.json';

await i18n.use(initReactI18next).init({
  resources: { en: { translation: en } },
  lng: 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});

type ProviderOptions = {
  initialEntries?: string[];
};

function createWrapper({ initialEntries = ['/'] }: ProviderOptions) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  };
}

type CustomRenderOptions = Omit<RenderOptions, 'wrapper'> & ProviderOptions;

export function renderWithProviders(ui: ReactElement, options: CustomRenderOptions = {}) {
  const { initialEntries, ...renderOptions } = options;
  return {
    user: userEvent.setup(),
    ...render(ui, {
      wrapper: createWrapper({ initialEntries }),
      ...renderOptions,
    }),
  };
}

export { screen, waitFor, within, act, userEvent };
