import { readdirSync, readFile } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

describe('storybook coverage contract', () => {
  it('contains current app-level UI stories (big to small)', () => {
    const storiesDir = dirname(fileURLToPath(import.meta.url));
    const files = readdirSync(storiesDir).filter((file) => file.endsWith('.stories.tsx'));

    expect(files).toEqual(
      expect.arrayContaining([
        'AppShell.stories.tsx',
        'AppNavigation.stories.tsx',
        'AppSurfaces.stories.tsx',
        'AppControls.stories.tsx',
        'AppFeedback.stories.tsx',
        'LocaleCurrencyEditor.stories.tsx',
        'TelegramPreview.stories.tsx',
        'TelegramPostParts.stories.tsx',
        'AuthGuard.stories.tsx',
        'BackButtonHandler.stories.tsx',
        'ErrorBoundary.stories.tsx',
      ]),
    );
  });

  it('keeps light and dark theme toolbar options in Storybook preview config', async () => {
    const previewPath = resolve(process.cwd(), '.storybook/preview.ts');
    const previewSource = await new Promise<string>((resolvePromise, rejectPromise) => {
      readFile(previewPath, 'utf-8', (error, data) => {
        if (error) {
          rejectPromise(error);
          return;
        }

        resolvePromise(data);
      });
    });

    expect(previewSource).toContain("value: 'light'");
    expect(previewSource).toContain("value: 'dark'");
  });
});
