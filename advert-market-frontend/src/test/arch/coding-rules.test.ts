import { describe, expect, it } from 'vitest';
import { getAllSourceFiles, getFileContent, getLayer, toRelative } from './arch-utils';

describe('Coding rules', () => {
  const allFiles = getAllSourceFiles();

  it('no fetch() outside shared/api/client.ts', () => {
    const violations: string[] = [];
    const allowed = 'shared/api/client.ts';

    for (const file of allFiles) {
      const rel = toRelative(file);
      if (rel === allowed) continue;

      const content = getFileContent(file);
      // Match standalone fetch( — not part of fetchProfile, fetchChannelTeam, etc.
      const fetchRe = /\bfetch\s*\(/g;
      for (let match = fetchRe.exec(content); match !== null; match = fetchRe.exec(content)) {
        const before = content.slice(Math.max(0, match.index - 1), match.index);
        if (/\w/.test(before)) continue;

        const line = content.slice(0, match.index).split('\n').length;
        violations.push(`${rel}:${line} uses fetch() directly`);
      }
    }

    expect(violations, `All HTTP calls should go through shared/api/client.ts:\n${violations.join('\n')}`).toEqual([]);
  });

  it('no native HTML elements in TSX outside shared/ui/', () => {
    const nativeElements = ['<button', '<input', '<select', '<textarea', '<img'];
    // Pre-existing violations to be fixed incrementally (ratchet down to zero)
    const GRANDFATHERED = new Set([
      'features/channels/components/CategoryChipRow.tsx',
      'features/channels/components/ChannelFiltersContent.tsx',
      'features/deals/components/DealTimeline.tsx',
      'features/onboarding/components/mini-timeline.tsx',
      'features/onboarding/components/role-card.tsx',
      'pages/catalog/CatalogPage.tsx',
      'pages/onboarding/OnboardingTourPage.tsx',
    ]);
    const violations: string[] = [];

    for (const file of allFiles) {
      if (!file.endsWith('.tsx')) continue;

      const rel = toRelative(file);
      if (rel.startsWith('shared/ui/')) continue;
      if (GRANDFATHERED.has(rel)) continue;

      const content = getFileContent(file);
      for (const tag of nativeElements) {
        const re = new RegExp(`${tag}[\\s/>]`, 'g');
        for (let match = re.exec(content); match !== null; match = re.exec(content)) {
          const line = content.slice(0, match.index).split('\n').length;
          violations.push(`${rel}:${line} uses native ${tag}> — use UI Kit component instead`);
        }
      }
    }

    expect(violations, `Use UI Kit components instead of native HTML elements:\n${violations.join('\n')}`).toEqual([]);
  });

  it('no direct window.Telegram in features/ and pages/', () => {
    const violations: string[] = [];

    for (const file of allFiles) {
      const layer = getLayer(file);
      if (layer !== 'features' && layer !== 'pages') continue;

      const content = getFileContent(file);
      const re = /window\.Telegram/g;
      for (let match = re.exec(content); match !== null; match = re.exec(content)) {
        const rel = toRelative(file);
        const line = content.slice(0, match.index).split('\n').length;
        violations.push(
          `${rel}:${line} accesses window.Telegram directly — use @telegram-apps/sdk-react or shared hooks`,
        );
      }
    }

    expect(
      violations,
      `Use @telegram-apps/sdk-react or shared hooks instead of window.Telegram:\n${violations.join('\n')}`,
    ).toEqual([]);
  });

  it('no console.log/warn/error in production code', () => {
    const violations: string[] = [];
    const allowedFiles = new Set(['shared/ui/components/error-boundary.tsx']);

    for (const file of allFiles) {
      const rel = toRelative(file);
      if (allowedFiles.has(rel)) continue;
      if (rel.includes('eruda')) continue;

      const content = getFileContent(file);
      const re = /\bconsole\.(log|warn|error)\b/g;
      for (let match = re.exec(content); match !== null; match = re.exec(content)) {
        const line = content.slice(0, match.index).split('\n').length;
        violations.push(`${rel}:${line} uses console.${match[1]}`);
      }
    }

    expect(violations, `Remove console statements from production code:\n${violations.join('\n')}`).toEqual([]);
  });
});
