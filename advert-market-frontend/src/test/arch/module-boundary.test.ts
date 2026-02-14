import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  getAllSourceFiles,
  getFeatureName,
  getImportFeatureName,
  getImportLayer,
  getImports,
  isShallowImport,
  SRC_DIR,
  toRelative,
} from './arch-utils';

describe('Module boundary rules', () => {
  const featureFiles = getAllSourceFiles(join(SRC_DIR, 'features'));

  it('features do not import from other features (cross-feature isolation)', () => {
    const violations: string[] = [];

    for (const file of featureFiles) {
      const currentFeature = getFeatureName(file);
      if (!currentFeature) continue;

      const imports = getImports(file);
      for (const imp of imports) {
        const importedFeature = getImportFeatureName(imp);
        if (importedFeature && importedFeature !== currentFeature) {
          violations.push(`${toRelative(file)} imports from feature "${importedFeature}" via ${imp}`);
        }
      }
    }

    expect(violations, `Features must not import from other features:\n${violations.join('\n')}`).toEqual([]);
  });

  it('features do not import deeply nested shared internals', () => {
    // Pre-existing deep imports from shared/ui compound module.
    // Remove entries as barrel exports are added to shared/ui/index.ts.
    const GRANDFATHERED_PATHS = new Set([
      '@/shared/ui/components/channel-avatar',
      '@/shared/ui/components/language-badge',
      '@/shared/ui/components/textarea-field',
    ]);
    const violations: string[] = [];

    for (const file of featureFiles) {
      const imports = getImports(file);
      for (const imp of imports) {
        const layer = getImportLayer(imp);
        if (layer !== 'shared') continue;
        if (GRANDFATHERED_PATHS.has(imp)) continue;

        if (!isShallowImport(imp)) {
          violations.push(`${toRelative(file)} imports deep shared internal: ${imp}`);
        }
      }
    }

    expect(
      violations,
      `Features should not reach into deeply nested shared internals (max 3 path segments):\n${violations.join('\n')}`,
    ).toEqual([]);
  });
});
