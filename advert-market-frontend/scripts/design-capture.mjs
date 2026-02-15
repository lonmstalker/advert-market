import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

function usage() {
  // eslint-disable-next-line no-console
  console.log(
    [
      'Usage:',
      '  node scripts/design-capture.mjs --phase before|after --theme light|dark',
      '',
      'Notes:',
      '- Stores screenshots under reports/design-fix/<YYYY-MM-DD>/<phase>/<theme>/',
      '- Sets CAPTURE_DIR and VITE_FORCE_THEME for Playwright webServer.',
      '- Forces a fresh webServer instance (no reuse) to ensure theme env is applied.',
    ].join('\n'),
  );
}

function parseArgs(argv) {
  const args = { phase: null, theme: null };
  for (let i = 0; i < argv.length; i += 1) {
    const a = argv[i];
    if (a === '--phase') args.phase = argv[i + 1] ?? null;
    if (a === '--theme') args.theme = argv[i + 1] ?? null;
  }
  return args;
}

function todayISO() {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

const { phase, theme } = parseArgs(process.argv.slice(2));
if (phase !== 'before' && phase !== 'after') {
  usage();
  process.exit(1);
}
if (theme !== 'light' && theme !== 'dark') {
  usage();
  process.exit(1);
}

const date = process.env.DESIGN_FIX_DATE ?? todayISO();
const outDir = path.join('reports', 'design-fix', date, phase, theme);
fs.mkdirSync(outDir, { recursive: true });

const env = {
  ...process.env,
  CAPTURE_DIR: outDir,
  VITE_FORCE_THEME: theme,
  // Ensure Playwright doesn't reuse an already-running Vite server with a different theme.
  PW_REUSE_SERVER: 'false',
};

const result = spawnSync(
  process.platform === 'win32' ? 'npx.cmd' : 'npx',
  // Capture is "best effort" and should be stable; run in a single engine and single worker to avoid
  // screenshot overwrites (the spec file names don't include project) and WebKit flakiness.
  ['playwright', 'test', 'e2e/design-fix-capture.spec.ts', '--project=chromium', '--workers=1'],
  { stdio: 'inherit', env },
);

process.exit(result.status ?? 1);
