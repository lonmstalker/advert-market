import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { Decorator } from '@storybook/react-vite';
import { ThemeProvider, ToastProvider } from '@telegram-tools/ui-kit';
import i18n from 'i18next';
import type { ReactNode } from 'react';
import { useMemo } from 'react';
import { I18nextProvider, initReactI18next } from 'react-i18next';
import { MemoryRouter } from 'react-router';
import en from '../src/shared/i18n/locales/en.json';
import ru from '../src/shared/i18n/locales/ru.json';

if (!i18n.isInitialized) {
  void i18n.use(initReactI18next).init({
    resources: {
      en: { translation: en },
      ru: { translation: ru },
    },
    lng: 'ru',
    fallbackLng: 'ru',
    interpolation: { escapeValue: false },
    initImmediate: false,
  });
}

function StorybookProviders({
  children,
  theme,
  route,
}: {
  children: ReactNode;
  theme: 'light' | 'dark';
  route: string;
}) {
  const queryClient = useMemo(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: false, gcTime: 0 },
          mutations: { retry: false },
        },
      }),
    [],
  );

  return (
    <ThemeProvider theme={theme}>
      <I18nextProvider i18n={i18n}>
        <ToastProvider>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[route]}>
              <div
                style={{
                  padding: 'max(16px, env(safe-area-inset-top)) max(16px, env(safe-area-inset-right)) max(16px, env(safe-area-inset-bottom)) max(16px, env(safe-area-inset-left))',
                  minHeight: '100dvh',
                  width: '100%',
                  backgroundColor: 'var(--color-background-secondary)',
                }}
              >
                {children}
              </div>
            </MemoryRouter>
          </QueryClientProvider>
        </ToastProvider>
      </I18nextProvider>
    </ThemeProvider>
  );
}

export const withTheme: Decorator = (Story, context) => {
  const theme = context.globals['theme'] as 'light' | 'dark' | undefined;
  const route = typeof context.parameters?.route === 'string' ? context.parameters.route : '/catalog';

  return (
    <StorybookProviders theme={theme ?? 'light'} route={route}>
      <Story />
    </StorybookProviders>
  );
};
