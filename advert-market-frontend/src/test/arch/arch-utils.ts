import { readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dir = dirname(fileURLToPath(import.meta.url));
export const SRC_DIR = resolve(__dir, '../../');

const EXCLUDED_DIRS = new Set(['node_modules', 'dist', '.storybook']);
const SOURCE_EXTENSIONS = new Set(['.ts', '.tsx']);
const TEST_MARKERS = ['.test.', '.stories.', '__tests__'];

export function getAllSourceFiles(dir: string = SRC_DIR): string[] {
  const files: string[] = [];

  function walk(currentDir: string) {
    for (const entry of readdirSync(currentDir)) {
      if (EXCLUDED_DIRS.has(entry) || entry.startsWith('.')) continue;

      const fullPath = join(currentDir, entry);
      const stat = statSync(fullPath);

      if (stat.isDirectory()) {
        walk(fullPath);
      } else {
        const hasSourceExt = SOURCE_EXTENSIONS.has(fullPath.slice(fullPath.lastIndexOf('.')));
        const isTest = TEST_MARKERS.some((m) => fullPath.includes(m));
        if (hasSourceExt && !isTest) {
          files.push(fullPath);
        }
      }
    }
  }

  walk(dir);
  return files;
}

export function getAllTestFiles(dir: string = SRC_DIR): string[] {
  const files: string[] = [];

  function walk(currentDir: string) {
    for (const entry of readdirSync(currentDir)) {
      if (EXCLUDED_DIRS.has(entry) || entry.startsWith('.')) continue;

      const fullPath = join(currentDir, entry);
      const stat = statSync(fullPath);

      if (stat.isDirectory()) {
        walk(fullPath);
      } else if (fullPath.includes('.test.')) {
        files.push(fullPath);
      }
    }
  }

  walk(dir);
  return files;
}

export function getImports(filePath: string): string[] {
  const content = readFileSync(filePath, 'utf-8');
  const imports: string[] = [];

  const staticRe = /(?:import|export)\s+.*?\s+from\s+['"]([^'"]+)['"]/g;
  for (let m = staticRe.exec(content); m !== null; m = staticRe.exec(content)) {
    imports.push(m[1]);
  }

  const sideEffectRe = /^\s*import\s+['"]([^'"]+)['"]/gm;
  for (let m = sideEffectRe.exec(content); m !== null; m = sideEffectRe.exec(content)) {
    imports.push(m[1]);
  }

  const dynamicRe = /import\(\s*['"]([^'"]+)['"]\s*\)/g;
  for (let m = dynamicRe.exec(content); m !== null; m = dynamicRe.exec(content)) {
    imports.push(m[1]);
  }

  return imports;
}

export function resolveAlias(importPath: string): string | null {
  if (importPath.startsWith('@/')) {
    return importPath.replace('@/', 'src/');
  }
  return null;
}

export type Layer = 'app' | 'pages' | 'features' | 'shared' | 'test' | 'unknown';

export function getLayer(filePath: string): Layer {
  const rel = toRelative(filePath);
  if (rel.startsWith('app/')) return 'app';
  if (rel.startsWith('pages/')) return 'pages';
  if (rel.startsWith('features/')) return 'features';
  if (rel.startsWith('shared/')) return 'shared';
  if (rel.startsWith('test/')) return 'test';
  return 'unknown';
}

export function getImportLayer(importPath: string): Layer | null {
  const resolved = resolveAlias(importPath);
  if (!resolved) return null;
  if (resolved.startsWith('src/app/')) return 'app';
  if (resolved.startsWith('src/pages/')) return 'pages';
  if (resolved.startsWith('src/features/')) return 'features';
  if (resolved.startsWith('src/shared/')) return 'shared';
  if (resolved.startsWith('src/test/')) return 'test';
  return 'unknown';
}

export function getFeatureName(filePath: string): string | null {
  const rel = toRelative(filePath);
  const match = rel.match(/^features\/([^/]+)/);
  return match ? match[1] : null;
}

export function getImportFeatureName(importPath: string): string | null {
  const resolved = resolveAlias(importPath);
  if (!resolved) return null;
  const match = resolved.match(/^src\/features\/([^/]+)/);
  return match ? match[1] : null;
}

export function getFileContent(filePath: string): string {
  return readFileSync(filePath, 'utf-8');
}

export function toRelative(filePath: string): string {
  return relative(SRC_DIR, filePath).replace(/\\/g, '/');
}

export function isShallowImport(importPath: string): boolean {
  const resolved = resolveAlias(importPath);
  if (!resolved) return true;
  const stripped = resolved.replace('src/', '');
  const segments = stripped.split('/');
  // Allow up to 3 segments: shared/module/file (e.g. @/shared/api/client)
  // Reject 4+: shared/module/internal/helper
  return segments.length <= 3;
}
