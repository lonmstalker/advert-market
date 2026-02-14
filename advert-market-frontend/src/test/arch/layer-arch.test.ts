import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { getAllSourceFiles, getImportLayer, getImports, SRC_DIR, toRelative } from './arch-utils';

describe('Layer architecture rules', () => {
  const sharedFiles = getAllSourceFiles(join(SRC_DIR, 'shared'));
  const featureFiles = getAllSourceFiles(join(SRC_DIR, 'features'));

  it('shared/ does not import from features/ or pages/', () => {
    const violations: string[] = [];

    for (const file of sharedFiles) {
      const imports = getImports(file);
      for (const imp of imports) {
        const layer = getImportLayer(imp);
        if (layer === 'features' || layer === 'pages') {
          violations.push(`${toRelative(file)} imports ${imp}`);
        }
      }
    }

    expect(violations, `Shared layer must not depend on features or pages:\n${violations.join('\n')}`).toEqual([]);
  });

  it('features/ does not import from pages/', () => {
    const violations: string[] = [];

    for (const file of featureFiles) {
      const imports = getImports(file);
      for (const imp of imports) {
        const layer = getImportLayer(imp);
        if (layer === 'pages') {
          violations.push(`${toRelative(file)} imports ${imp}`);
        }
      }
    }

    expect(violations, `Features layer must not depend on pages:\n${violations.join('\n')}`).toEqual([]);
  });

  it('app/ does not statically import from pages/ (only lazy imports allowed)', () => {
    const appFiles = getAllSourceFiles(join(SRC_DIR, 'app'));
    const violations: string[] = [];

    for (const file of appFiles) {
      const imports = getImports(file);
      const fileText = readFileSync(file, 'utf-8');

      for (const imp of imports) {
        const layer = getImportLayer(imp);
        if (layer !== 'pages') continue;

        const escaped = imp.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const staticImportRe = new RegExp(`(?:import|export)\\s+.*?from\\s+['"]${escaped}['"]`);
        if (staticImportRe.test(fileText)) {
          violations.push(`${toRelative(file)} has static import of page: ${imp}`);
        }
      }
    }

    expect(
      violations,
      `App layer should only use dynamic imports (React.lazy) for pages:\n${violations.join('\n')}`,
    ).toEqual([]);
  });
});
