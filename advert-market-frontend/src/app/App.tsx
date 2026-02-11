import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Spinner, ThemeProvider, ToastProvider } from '@telegram-tools/ui-kit';
import { useLaunchParams } from '@telegram-apps/sdk-react';
import { TonConnectUIProvider } from '@tonconnect/ui-react';
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

const TON_MANIFEST_URL = `${window.location.origin}/tonconnect-manifest.json`;

export function App() {
  const lp = useLaunchParams(true);
  const theme = lp.themeParams?.isDark ? 'dark' : 'light';

  return (
    <TonConnectUIProvider manifestUrl={TON_MANIFEST_URL}>
      <ThemeProvider theme={theme}>
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
    </TonConnectUIProvider>
  );
}

function PageLoader() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <Spinner size="32px" color="accent" />
    </div>
  );
}