import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const EMOJI_REGEX =
  /[\u{1F300}-\u{1F9FF}\u{2600}-\u{27BF}\u{FE00}-\u{FE0F}\u{200D}\u{1F1E0}-\u{1F1FF}\u{2702}-\u{27B0}\u{1FA00}-\u{1FAD6}\u{2B50}\u{23CF}-\u{23F3}\u{2934}\u{2935}\u{25AA}\u{25AB}\u{25B6}\u{25C0}\u{25FB}-\u{25FE}\u{2614}\u{2615}\u{2648}-\u{2653}\u{267F}\u{2693}\u{26A1}\u{26AA}\u{26AB}\u{26BD}\u{26BE}\u{26C4}\u{26C5}\u{26CE}\u{26D4}\u{26EA}\u{26F2}\u{26F3}\u{26F5}\u{26FA}\u{26FD}\u{2705}\u{270A}-\u{270D}\u{2728}\u{274C}\u{274E}\u{2753}-\u{2755}\u{2757}\u{2795}-\u{2797}\u{27A1}\u{27B0}]/u;

const EXTENSIONS = new Set(['.ts', '.tsx', '.json']);
const IGNORE_DIRS = new Set(['node_modules', 'dist', '.git', 'coverage', 'storybook-static']);

function walk(dir: string): string[] {
  const files: string[] = [];
  for (const entry of readdirSync(dir)) {
    if (IGNORE_DIRS.has(entry)) continue;
    const full = join(dir, entry);
    const stat = statSync(full);
    if (stat.isDirectory()) {
      files.push(...walk(full));
    } else if (EXTENSIONS.has(full.slice(full.lastIndexOf('.')))) {
      files.push(full);
    }
  }
  return files;
}

const root = join(import.meta.dirname, '..', 'src');
const files = walk(root);
let violations = 0;

for (const file of files) {
  const content = readFileSync(file, 'utf-8');
  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const match = EMOJI_REGEX.exec(lines[i]);
    if (match) {
      const rel = relative(join(import.meta.dirname, '..'), file);
      console.error(`${rel}:${i + 1}:${match.index + 1} — found emoji: ${match[0]} (U+${match[0].codePointAt(0)?.toString(16).toUpperCase()})`);
      violations++;
    }
  }
}

if (violations > 0) {
  console.error(`\nFound ${violations} emoji violation(s). Use SVG icons instead.`);
  process.exit(1);
} else {
  console.log('No emoji found in source files.');
}
