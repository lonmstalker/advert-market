import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { SRC_DIR } from './arch-utils';

const FEATURES_DIR = join(SRC_DIR, 'features');
const PAGES_DIR = join(SRC_DIR, 'pages');

const ALLOWED_FEATURE_SUBDIRS = new Set(['api', 'types', 'hooks', 'components', 'lib', 'store']);

function getDirectories(dir: string): string[] {
  if (!existsSync(dir)) return [];
  return readdirSync(dir).filter((entry) => statSync(join(dir, entry)).isDirectory());
}

describe('Feature structure rules', () => {
  const features = getDirectories(FEATURES_DIR);

  it('each feature has an index.ts barrel export', () => {
    const violations: string[] = [];

    for (const feature of features) {
      const indexPath = join(FEATURES_DIR, feature, 'index.ts');
      if (!existsSync(indexPath)) {
        violations.push(`features/${feature}/ is missing index.ts barrel export`);
      }
    }

    expect(violations, `Every feature must have an index.ts barrel export:\n${violations.join('\n')}`).toEqual([]);
  });

  it('feature subdirectories are from the allowed set', () => {
    const violations: string[] = [];

    for (const feature of features) {
      const subdirs = getDirectories(join(FEATURES_DIR, feature));
      for (const subdir of subdirs) {
        if (subdir.startsWith('__')) continue; // __tests__ is ok
        if (!ALLOWED_FEATURE_SUBDIRS.has(subdir)) {
          violations.push(
            `features/${feature}/${subdir}/ is not in allowed set: ${[...ALLOWED_FEATURE_SUBDIRS].join(', ')}`,
          );
        }
      }
    }

    expect(violations, `Feature subdirectories must be from the allowed set:\n${violations.join('\n')}`).toEqual([]);
  });

  it('page files use PascalCase with Page.tsx suffix', () => {
    const violations: string[] = [];
    const pageGroups = getDirectories(PAGES_DIR);

    for (const group of pageGroups) {
      const groupDir = join(PAGES_DIR, group);
      const entries = readdirSync(groupDir);

      for (const entry of entries) {
        const fullPath = join(groupDir, entry);
        if (statSync(fullPath).isDirectory()) continue;
        if (!entry.endsWith('.tsx')) continue;
        if (entry.includes('.test.') || entry.includes('.stories.')) continue;

        if (!/^[A-Z][a-zA-Z]*Page\.tsx$/.test(entry)) {
          violations.push(`pages/${group}/${entry} should match PascalCasePage.tsx`);
        }
      }
    }

    expect(violations, `Page files must be PascalCase with Page.tsx suffix:\n${violations.join('\n')}`).toEqual([]);
  });

  it('page files have default export', () => {
    const violations: string[] = [];
    const pageGroups = getDirectories(PAGES_DIR);

    for (const group of pageGroups) {
      const groupDir = join(PAGES_DIR, group);
      const entries = readdirSync(groupDir);

      for (const entry of entries) {
        if (!entry.endsWith('.tsx')) continue;
        if (entry.includes('.test.') || entry.includes('.stories.')) continue;
        if (!/^[A-Z]/.test(entry)) continue;

        const content = readFileSync(join(groupDir, entry), 'utf-8');
        if (!content.includes('export default')) {
          violations.push(`pages/${group}/${entry} is missing a default export (required for React.lazy)`);
        }
      }
    }

    expect(violations, `Page files must have a default export for React.lazy:\n${violations.join('\n')}`).toEqual([]);
  });
});
