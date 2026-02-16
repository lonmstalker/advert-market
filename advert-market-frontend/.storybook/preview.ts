import type { Preview } from '@storybook/react-vite';
import '@telegram-tools/ui-kit/dist/index.css';
import '../src/app/tailwind.css';
import '../src/app/components.css';
import '../src/app/global.css';
import { withTheme } from './decorators';

const preview: Preview = {
  decorators: [withTheme],
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    a11y: {
      test: 'todo',
    },
    layout: 'centered',
  },
  globalTypes: {
    theme: {
      description: 'Theme mode',
      toolbar: {
        title: 'Theme',
        items: [
          { value: 'light', icon: 'sun', title: 'Light' },
          { value: 'dark', icon: 'moon', title: 'Dark' },
        ],
        dynamicTitle: true,
      },
    },
  },
  initialGlobals: {
    theme: 'light',
  },
};

export default preview;