import { extname } from 'node:path';
import { describe, expect, it } from 'vitest';
import { getAllSourceFiles, getFileContent, toRelative } from './arch-utils';

const RAW_CONTROL_ALLOWLIST = new Set([
  // UI Kit does not provide textarea API, so we keep dedicated textarea wrappers.
  'shared/ui/components/textarea.tsx',
  'shared/ui/components/textarea-field.tsx',
  // Native file picker is required for local file upload.
  'features/creatives/components/MediaItemList.tsx',
  // FilterButton uses raw <button> inside motion.div for specialized press animation.
  'shared/ui/components/filter-button.tsx',
  // LocaleCurrencyEditor uses inline text-link <button> for "reset to auto" and "undo" actions.
  'shared/ui/components/locale-currency-editor.tsx',
  // Popover uses raw <button> for trigger with custom positioning logic.
  'shared/ui/components/popover.tsx',
  // TelegramSpoiler uses raw <button> for spoiler reveal interaction.
  'shared/ui/components/telegram-post-preview/TelegramSpoiler.tsx',
]);

const MOTION_BUTTON_ALLOWLIST = new Set([
  // Tappable is the single centralized wrapper for press animation and button semantics.
  'shared/ui/components/tappable.tsx',
  // SegmentControl uses motion.button for tab switching with pressScale animation + role="tab".
  'shared/ui/components/segment-control.tsx',
]);

const UI_KIT_REQUIRED_COMPONENTS = [
  'shared/ui/components/search-input.tsx',
  'shared/ui/components/chip.tsx',
  'shared/ui/components/segment-control.tsx',
  'shared/ui/components/locale-currency-editor.tsx',
  'shared/ui/components/telegram-post-preview/TelegramChatSimulator.tsx',
  'shared/ui/components/telegram-post-preview/TelegramPostHeader.tsx',
  'shared/ui/components/telegram-post-preview/TelegramPostMedia.tsx',
  // Excluded: filter-button.tsx (uses motion + icon, no UI Kit text/button needed)
  // Excluded: TelegramSpoiler.tsx (raw button for spoiler reveal, no UI Kit equivalent)
] as const;

const RAW_CONTROL_PATTERNS = [
  { label: '<button>', pattern: /<button\b/ },
  { label: '<input>', pattern: /<input\b/ },
  { label: '<textarea>', pattern: /<textarea\b/ },
  { label: '<img>', pattern: /<img\b/ },
  { label: '<select>', pattern: /<select\b/ },
] as const;

const MOTION_BUTTON_PATTERN = /motion\.button\b/;

describe('UI kit usage contract', () => {
  const sourceFiles = getAllSourceFiles();

  it('uses UI kit in target ad-market shared UI components', () => {
    const sourceByRelativePath = new Map<string, string>(
      sourceFiles.map((file) => [toRelative(file), getFileContent(file)]),
    );
    const violations: string[] = [];

    for (const file of UI_KIT_REQUIRED_COMPONENTS) {
      const content = sourceByRelativePath.get(file);
      if (!content) {
        violations.push(`${file} is missing`);
        continue;
      }

      if (!content.includes("from '@telegram-tools/ui-kit'")) {
        violations.push(`${file} does not import @telegram-tools/ui-kit`);
      }
    }

    expect(violations, `Target shared UI components must use UI Kit primitives:\n${violations.join('\n')}`).toEqual([]);
  });

  it('does not use raw HTML controls in source files (except approved native-only cases)', () => {
    const violations: string[] = [];

    for (const file of sourceFiles) {
      if (extname(file) !== '.tsx') continue;

      const relativePath = toRelative(file);
      if (RAW_CONTROL_ALLOWLIST.has(relativePath)) continue;

      const content = getFileContent(file);
      for (const { label, pattern } of RAW_CONTROL_PATTERNS) {
        if (pattern.test(content)) {
          violations.push(`${relativePath} contains ${label}`);
        }
      }

      if (MOTION_BUTTON_PATTERN.test(content) && !MOTION_BUTTON_ALLOWLIST.has(relativePath)) {
        violations.push(`${relativePath} contains motion.button (must use Tappable wrapper)`);
      }
    }

    expect(violations, `Raw HTML controls are not allowed outside approved cases:\n${violations.join('\n')}`).toEqual(
      [],
    );
  });
});
