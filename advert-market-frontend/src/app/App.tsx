import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Spinner, ThemeProvider, ToastProvider } from '@telegram-tools/ui-kit';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router';

const HomePage = lazy(() => import('@/pages/home/HomePage'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function getTelegramTheme(): 'light' | 'dark' | undefined {
  return window.Telegram?.WebApp.colorScheme;
}

export function App() {
  return (
    <ThemeProvider theme={getTelegramTheme()}>
      <ToastProvider>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <Suspense fallback={<PageLoader />}>
              <Routes>
                <Route path="/" element={<HomePage />} />
              </Routes>
            </Suspense>
          </BrowserRouter>
        </QueryClientProvider>
      </ToastProvider>
    </ThemeProvider>
  );
}

function PageLoader() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <Spinner size="32px" color="accent" />
    </div>
  );
}
