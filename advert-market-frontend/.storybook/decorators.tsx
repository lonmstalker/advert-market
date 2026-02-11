import type { Decorator } from '@storybook/react-vite';
import { ThemeProvider, ToastProvider } from '@telegram-tools/ui-kit';

export const withTheme: Decorator = (Story, context) => {
  const theme = context.globals['theme'] as 'light' | 'dark' | undefined;
  return (
    <ThemeProvider theme={theme ?? 'light'}>
      <ToastProvider>
        <div
          style={{
            padding: '20px',
            minWidth: '360px',
            minHeight: '100vh',
            backgroundColor: 'var(--color-background-secondary)',
          }}
        >
          <Story />
        </div>
      </ToastProvider>
    </ThemeProvider>
  );
};