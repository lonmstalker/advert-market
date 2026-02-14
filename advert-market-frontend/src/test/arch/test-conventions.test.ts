import { readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { getAllTestFiles, getFileContent, SRC_DIR, toRelative } from './arch-utils';

function findSpecFiles(dir: string): string[] {
  const results: string[] = [];

  function walk(currentDir: string) {
    for (const entry of readdirSync(currentDir)) {
      if (entry === 'node_modules' || entry === 'dist') continue;

      const fullPath = join(currentDir, entry);
      const stat = statSync(fullPath);

      if (stat.isDirectory()) {
        walk(fullPath);
      } else if (entry.includes('.spec.')) {
        results.push(fullPath);
      }
    }
  }

  walk(dir);
  return results;
}

describe('Test conventions', () => {
  const testFiles = getAllTestFiles();

  it('test files use .test.{ts,tsx} naming (not .spec.*)', () => {
    const specFiles = findSpecFiles(SRC_DIR);
    const violations = specFiles.map((f) => `${toRelative(f)} should use .test. instead of .spec.`);

    expect(violations, `Use .test.{ts,tsx} naming convention:\n${violations.join('\n')}`).toEqual([]);
  });

  it('test files contain describe()', () => {
    const violations: string[] = [];

    for (const file of testFiles) {
      const content = getFileContent(file);
      if (!content.includes('describe(')) {
        violations.push(`${toRelative(file)} is missing describe() block`);
      }
    }

    expect(violations, `Test files should contain describe() blocks:\n${violations.join('\n')}`).toEqual([]);
  });

  it('no .only() in committed tests', () => {
    const violations: string[] = [];

    for (const file of testFiles) {
      const content = getFileContent(file);
      // Only match vitest/jest test runner .only() — not assertion methods
      const re = /\b(?:describe|it|test)\.only\s*\(/g;
      for (let match = re.exec(content); match !== null; match = re.exec(content)) {
        const line = content.slice(0, match.index).split('\n').length;
        violations.push(`${toRelative(file)}:${line} contains .only() — remove before committing`);
      }
    }

    expect(violations, `Remove .only() from committed tests:\n${violations.join('\n')}`).toEqual([]);
  });

  it('test utilities are imported from @/test/test-utils', () => {
    // Pre-existing violations — tests that import testing-library directly.
    // Remove entries from this set as tests are migrated to @/test/test-utils.
    const GRANDFATHERED = new Set([
      'features/channels/hooks/__tests__/useChannelFilters.test.ts',
      'features/channels/hooks/__tests__/useChannelRights.test.tsx',
      'features/deals/hooks/__tests__/useDealDetail.test.tsx',
      'features/deals/hooks/__tests__/useDealTransition.test.tsx',
      'pages/onboarding/OnboardingTourPage.test.tsx',
      'shared/hooks/__tests__/use-countdown.test.ts',
      'shared/hooks/__tests__/use-infinite-scroll.test.tsx',
      'shared/hooks/use-debounce.test.ts',
      'shared/hooks/use-haptic.test.ts',
      'shared/hooks/use-telegram.test.ts',
      'shared/ui/components/__tests__/channel-avatar.test.tsx',
      'shared/ui/components/__tests__/end-of-list.test.tsx',
      'shared/ui/components/__tests__/formatted-price.test.tsx',
      'shared/ui/components/__tests__/language-badge.test.tsx',
      'shared/ui/components/__tests__/page-loader.test.tsx',
      'shared/ui/components/__tests__/popover.test.tsx',
      'shared/ui/components/__tests__/segment-control.test.tsx',
      'shared/ui/components/__tests__/textarea-field.test.tsx',
      'shared/ui/icons/post-type-icons.test.tsx',
    ]);
    const violations: string[] = [];
    const directImports = ['@testing-library/react', '@testing-library/user-event'];

    for (const file of testFiles) {
      const rel = toRelative(file);
      if (rel.startsWith('test/')) continue;
      if (GRANDFATHERED.has(rel)) continue;

      const content = getFileContent(file);
      for (const directImport of directImports) {
        if (content.includes(`from '${directImport}'`) || content.includes(`from "${directImport}"`)) {
          violations.push(`${rel} imports ${directImport} directly — use @/test/test-utils instead`);
        }
      }
    }

    expect(violations, `Import test utilities from @/test/test-utils:\n${violations.join('\n')}`).toEqual([]);
  });
});
