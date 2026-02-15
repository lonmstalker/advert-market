#!/usr/bin/env node

import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const repoRoot = process.cwd();
const configPath = path.resolve(repoRoot, 'scripts/cross-consistency.config.json');

if (!fs.existsSync(configPath)) {
  console.error(`Config not found: ${configPath}`);
  process.exit(1);
}

const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
const findings = [];
const baselineArg = process.argv.find((arg) => arg.startsWith('--baseline=')) ?? '';
const baseline = baselineArg ? baselineArg.split('=')[1] : null;

function addFinding(scope, message) {
  findings.push(`[${scope}] ${message}`);
}

function rel(filePath) {
  return path.relative(repoRoot, filePath).replaceAll(path.sep, '/');
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

function walkFiles(rootAbs, predicate) {
  if (!fs.existsSync(rootAbs)) {
    return [];
  }
  const out = [];
  const entries = fs.readdirSync(rootAbs, { withFileTypes: true });
  for (const entry of entries) {
    const abs = path.join(rootAbs, entry.name);
    if (entry.isDirectory()) {
      out.push(...walkFiles(abs, predicate));
      continue;
    }
    if (entry.isFile() && predicate(abs)) {
      out.push(abs);
    }
  }
  return out;
}

function normalizePath(rawPath) {
  const [noQuery] = rawPath.split('?');
  const trimmed = noQuery.trim();
  const withLeadingSlash = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  const withTemplateParams = withLeadingSlash
    .replaceAll(/\$\{[^}]+\}/g, '{param}')
    .replaceAll(/\{[^/}]+\}/g, '{param}');
  const normalized = withTemplateParams.replaceAll(/\/+/g, '/');
  if (normalized.length > 1 && normalized.endsWith('/')) {
    return normalized.slice(0, -1);
  }
  return normalized;
}

function endpointKey(method, rawPath) {
  return `${method.toUpperCase()} ${normalizePath(rawPath)}`;
}

function lineOf(content, index) {
  return content.slice(0, index).split(/\r?\n/u).length;
}

function stripTicks(value) {
  return value.replaceAll('`', '').trim();
}

function parseBackendEndpoints() {
  const controllers = [];
  for (const rootRel of config.backendControllerRoots ?? []) {
    const rootAbs = path.resolve(repoRoot, rootRel);
    controllers.push(
      ...walkFiles(rootAbs, (filePath) => filePath.endsWith('Controller.java')),
    );
  }

  const endpointMap = new Map();
  const methodByAnnotation = new Map([
    ['GetMapping', 'GET'],
    ['PostMapping', 'POST'],
    ['PutMapping', 'PUT'],
    ['DeleteMapping', 'DELETE'],
    ['PatchMapping', 'PATCH'],
  ]);

  for (const controller of controllers) {
    const content = readText(controller);
    const classIndex = content.indexOf('class ');
    const preClassContent = classIndex > 0 ? content.slice(0, classIndex) : content;
    const classRequestMappings = [...preClassContent.matchAll(/@RequestMapping\(\s*"([^"]+)"\s*\)/g)];
    const basePath = classRequestMappings.length > 0 ? classRequestMappings.at(-1)[1] : '';

    const mappingRegex = /@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)(?:\(([^)]*)\))?/g;
    let match;
    while ((match = mappingRegex.exec(content)) !== null) {
      const annotation = match[1];
      const method = methodByAnnotation.get(annotation);
      if (!method) {
        continue;
      }
      const inner = match[2] ?? '';
      const pathMatch = inner.match(/"([^"]*)"/);
      const subPath = pathMatch ? pathMatch[1] : '';
      const combined = normalizePath(`${basePath}${subPath}`);
      if (!combined.startsWith('/api/v1') && !combined.startsWith('/internal/v1')) {
        continue;
      }
      const key = endpointKey(method, combined);
      endpointMap.set(key, {
        method,
        path: normalizePath(combined),
        file: controller,
        line: lineOf(content, match.index),
      });
    }
  }

  return endpointMap;
}

function parseFrontendEndpoints() {
  const files = [];
  for (const rootRel of config.frontendApiRoots ?? []) {
    const rootAbs = path.resolve(repoRoot, rootRel);
    files.push(...walkFiles(rootAbs, (filePath) => filePath.endsWith('.ts') || filePath.endsWith('.tsx')));
  }

  const endpointMap = new Map();
  const regex = /api\s*\.\s*(get|post|put|delete)\s*\(\s*([`'"])(\/[\s\S]*?)\2/gm;
  for (const filePath of files) {
    const content = readText(filePath);
    let match;
    while ((match = regex.exec(content)) !== null) {
      const method = match[1].toUpperCase();
      const rawPath = match[3].trim();
      const withPrefix = rawPath.startsWith('/api/')
        ? rawPath
        : `/api/v1${rawPath}`;
      const key = endpointKey(method, withPrefix);
      endpointMap.set(key, {
        method,
        path: normalizePath(withPrefix),
        file: filePath,
        line: lineOf(content, match.index),
      });
    }
  }

  return endpointMap;
}

function sectionBetween(content, heading) {
  const headingRegex = new RegExp(`^##\\s+${heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`, 'm');
  const startMatch = headingRegex.exec(content);
  if (!startMatch) {
    return null;
  }
  const start = startMatch.index + startMatch[0].length;
  const rest = content.slice(start);
  const nextHeadingMatch = /^\n##\s+/m.exec(rest);
  const end = nextHeadingMatch ? start + nextHeadingMatch.index : content.length;
  return content.slice(start, end);
}

function parseContractTable(sectionText, expectedColumns) {
  const rows = [];
  if (!sectionText) {
    return rows;
  }
  const lines = sectionText.split(/\r?\n/u);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed.startsWith('|')) {
      continue;
    }
    if (trimmed.includes('---')) {
      continue;
    }
    const columns = trimmed
      .split('|')
      .map((chunk) => chunk.trim())
      .filter((chunk) => chunk.length > 0);
    if (columns.length < expectedColumns) {
      continue;
    }
    const method = stripTicks(columns[0]).toUpperCase();
    const endpointPath = stripTicks(columns[1]);
    if (!['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
      continue;
    }
    if (!endpointPath.startsWith('/')) {
      continue;
    }
    rows.push({
      method,
      path: normalizePath(endpointPath),
      columns,
      raw: trimmed,
    });
  }
  return rows;
}

function parseMemoryContracts() {
  const contractsRel = config.memoryBankContractsFile ?? '.memory-bank/11-api-contracts.md';
  const contractsAbs = path.resolve(repoRoot, contractsRel);
  if (!fs.existsSync(contractsAbs)) {
    addFinding('Setup', `Memory bank contracts file not found: ${contractsRel}`);
    return {
      implementedRows: [],
      plannedRows: [],
      contractsRel,
    };
  }
  const content = readText(contractsAbs);
  const implementedSection = sectionBetween(content, 'Implemented API (HEAD)');
  const plannedSection = sectionBetween(content, 'Planned API');

  if (!implementedSection) {
    addFinding('MemoryBank', `${contractsRel} is missing section: "## Implemented API (HEAD)"`);
  }
  if (!plannedSection) {
    addFinding('MemoryBank', `${contractsRel} is missing section: "## Planned API"`);
  }

  const implementedRows = parseContractTable(implementedSection, 2);
  const plannedRows = parseContractTable(plannedSection, 5).map((row) => ({
    ...row,
    beadsId: stripTicks(row.columns[3]),
    targetModule: stripTicks(row.columns[4]),
  }));

  return {
    implementedRows,
    plannedRows,
    contractsRel,
  };
}

function resolveBeadsRootAbs() {
  const beadsRootAbs = path.resolve(repoRoot, '.beads');
  const redirectAbs = path.resolve(beadsRootAbs, 'redirect');
  if (!fs.existsSync(redirectAbs)) {
    return beadsRootAbs;
  }
  const target = readText(redirectAbs).trim();
  if (!target) {
    return beadsRootAbs;
  }
  return path.isAbsolute(target)
    ? target
    : path.resolve(path.dirname(redirectAbs), target);
}

function loadBeadsStatuses() {
  const statusMap = new Map();
  const beadsRootAbs = resolveBeadsRootAbs();
  const issuesJsonlAbs = path.resolve(beadsRootAbs, 'issues.jsonl');

  if (fs.existsSync(issuesJsonlAbs)) {
    const lines = readText(issuesJsonlAbs).split(/\r?\n/u);
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]?.trim();
      if (!line) {
        continue;
      }
      let issue;
      try {
        issue = JSON.parse(line);
      } catch {
        addFinding('Beads', `Failed to parse ${rel(issuesJsonlAbs)}:${i + 1} as JSON`);
        continue;
      }
      if (issue?.id && issue?.status) {
        statusMap.set(issue.id, issue.status);
      }
    }
    return statusMap;
  }

  try {
    const raw = execSync('bd list --limit 0 --json', {
      cwd: repoRoot,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    const issues = JSON.parse(raw);
    for (const issue of issues) {
      statusMap.set(issue.id, issue.status);
    }
  } catch (error) {
    addFinding('Beads', `Failed to load Beads issues: ${String(error.message ?? error)}`);
  }
  return statusMap;
}

function stripQuotes(value) {
  const trimmed = value.trim();
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1);
  }
  return trimmed;
}

function parseOpenApiEndpoints() {
  const openApiRel = config.openApiSpecFile ?? 'api/openapi.yml';
  const openApiAbs = path.resolve(repoRoot, openApiRel);
  if (!fs.existsSync(openApiAbs)) {
    addFinding('Setup', `OpenAPI spec not found: ${openApiRel}`);
    return {
      openApiRel,
      endpoints: new Map(),
      paths: new Set(),
    };
  }

  const endpoints = new Map();
  const paths = new Set();
  const lines = readText(openApiAbs).split(/\r?\n/u);
  let inPaths = false;
  let currentPath = null;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (!inPaths) {
      if (/^paths:\s*$/u.test(line)) {
        inPaths = true;
      }
      continue;
    }

    if (!line.trim()) {
      continue;
    }
    if (/^\S/u.test(line)) {
      break; // leaving top-level `paths:`
    }

    const pathMatch = line.match(/^ {2}(['"]?\/[^'"]+['"]?):\s*$/u);
    if (pathMatch) {
      currentPath = stripQuotes(pathMatch[1]);
      paths.add(normalizePath(currentPath));
      continue;
    }

    if (!currentPath) {
      continue;
    }

    const methodMatch = line.match(/^ {4}(get|post|put|delete|patch):\s*$/u);
    if (methodMatch) {
      const method = methodMatch[1].toUpperCase();
      const key = endpointKey(method, currentPath);
      endpoints.set(key, {
        method,
        path: normalizePath(currentPath),
        file: openApiAbs,
        line: i + 1,
      });
    }
  }

  if (!inPaths) {
    addFinding('OpenAPI', `${openApiRel} is missing top-level "paths:" section`);
  }

  return {
    openApiRel,
    endpoints,
    paths,
  };
}

function parseFrontendSchemaPaths() {
  const schemaRel = config.frontendSchemaFile ?? 'advert-market-frontend/src/shared/api/schema.gen.ts';
  const schemaAbs = path.resolve(repoRoot, schemaRel);
  if (!fs.existsSync(schemaAbs)) {
    addFinding('Setup', `Frontend schema not found: ${schemaRel}`);
    return {
      schemaRel,
      schemaPaths: new Set(),
    };
  }

  const schemaPaths = new Set();
  const content = readText(schemaAbs);
  const regex = /^\s+['"](\/(?:api|internal)\/v1\/[^'"]+)['"]:\s*\{/gmu;
  let match;
  while ((match = regex.exec(content)) !== null) {
    schemaPaths.add(normalizePath(match[1]));
  }

  if (schemaPaths.size === 0) {
    addFinding('FrontendSchema', `${schemaRel} does not contain any /api/v1 or /internal/v1 path keys`);
  }

  return {
    schemaRel,
    schemaPaths,
  };
}

function parseAllowlist() {
  const allowlist = config.allowlist ?? [];
  const map = new Map();
  for (const entry of allowlist) {
    if (!entry.method || !entry.path) {
      addFinding('Allowlist', 'Entry must include method and path');
      continue;
    }
    if (!entry.beads_id) {
      addFinding('Allowlist', `Missing beads_id for ${entry.method} ${entry.path}`);
    }
    if (!entry.memory_bank_ref) {
      addFinding('Allowlist', `Missing memory_bank_ref for ${entry.method} ${entry.path}`);
    } else {
      const filePart = entry.memory_bank_ref.split('#')[0];
      const refAbs = path.resolve(repoRoot, filePart);
      if (!fs.existsSync(refAbs)) {
        addFinding('Allowlist', `memory_bank_ref target does not exist for ${entry.method} ${entry.path}: ${filePart}`);
      }
    }
    const key = endpointKey(entry.method, entry.path);
    map.set(key, entry);
  }
  return map;
}

const backendEndpoints = parseBackendEndpoints();
const frontendEndpoints = parseFrontendEndpoints();
const { implementedRows, plannedRows, contractsRel } = parseMemoryContracts();
const { openApiRel, endpoints: openApiEndpoints, paths: openApiPaths } = parseOpenApiEndpoints();
const { schemaRel, schemaPaths } = parseFrontendSchemaPaths();
const beadsStatuses = loadBeadsStatuses();
const allowlist = parseAllowlist();
const allowedBeadsStatuses = new Set(config.beadsValidStatuses ?? ['open', 'in_progress', 'blocked']);

const plannedMap = new Map(
  plannedRows.map((row) => [endpointKey(row.method, row.path), row]),
);

const implementedMap = new Map(
  implementedRows.map((row) => [endpointKey(row.method, row.path), row]),
);

for (const row of implementedRows) {
  const key = endpointKey(row.method, row.path);
  if (!backendEndpoints.has(key)) {
    addFinding(
      'MemoryBank->Backend',
      `${key} listed as implemented in ${contractsRel}, but backend endpoint is missing`,
    );
  }
}

for (const [key, endpoint] of backendEndpoints.entries()) {
  if (!implementedMap.has(key)) {
    const location = `${rel(endpoint.file)}:${endpoint.line}`;
    addFinding(
      'Backend->MemoryBank',
      `${key} exists in backend at ${location}, but is missing from implemented API section of ${contractsRel}`,
    );
  }
}

for (const [key, endpoint] of backendEndpoints.entries()) {
  if (openApiEndpoints.has(key)) {
    continue;
  }
  const location = `${rel(endpoint.file)}:${endpoint.line}`;
  addFinding(
    'Backend->OpenAPI',
    `${key} exists in backend at ${location}, but is missing from ${openApiRel}`,
  );
}

for (const [key, endpoint] of openApiEndpoints.entries()) {
  if (backendEndpoints.has(key)) {
    continue;
  }
  const location = `${rel(endpoint.file)}:${endpoint.line}`;
  addFinding(
    'OpenAPI->Backend',
    `${key} exists in ${location}, but is missing in backend controllers`,
  );
}

for (const openApiPath of openApiPaths) {
  if (!schemaPaths.has(openApiPath)) {
    addFinding(
      'OpenAPI->FrontendSchema',
      `${openApiPath} exists in ${openApiRel}, but is missing from ${schemaRel}`,
    );
  }
}

for (const schemaPath of schemaPaths) {
  if (!openApiPaths.has(schemaPath)) {
    addFinding(
      'FrontendSchema->OpenAPI',
      `${schemaPath} exists in ${schemaRel}, but is missing from ${openApiRel}`,
    );
  }
}

for (const row of plannedRows) {
  const key = endpointKey(row.method, row.path);
  if (backendEndpoints.has(key)) {
    addFinding(
      'MemoryBank->Backend',
      `${key} is listed as planned in ${contractsRel}, but already exists in backend (move to implemented section)`,
    );
  }
  if (!row.beadsId) {
    addFinding('PlannedCoverage', `${key} in planned section is missing Beads ID`);
    continue;
  }
  const status = beadsStatuses.get(row.beadsId);
  if (!status) {
    addFinding('PlannedCoverage', `${key} references unknown Beads issue: ${row.beadsId}`);
    continue;
  }
  if (!allowedBeadsStatuses.has(status)) {
    addFinding(
      'PlannedCoverage',
      `${key} references Beads issue ${row.beadsId} with non-active status: ${status}`,
    );
  }
  if (!row.targetModule) {
    addFinding('PlannedCoverage', `${key} in planned section is missing target module`);
  }
}

for (const [key, endpoint] of frontendEndpoints.entries()) {
  if (backendEndpoints.has(key)) {
    continue;
  }
  if (allowlist.has(key)) {
    continue;
  }
  const location = `${rel(endpoint.file)}:${endpoint.line}`;
  addFinding('Frontend->Backend', `${key} used in frontend at ${location}, but endpoint is missing in backend and not allowlisted`);
}

for (const [key, entry] of allowlist.entries()) {
  if (backendEndpoints.has(key)) {
    addFinding('Allowlist', `${key} is allowlisted but endpoint already exists in backend`);
    continue;
  }
  if (entry.scope === 'frontend-backend') {
    if (!frontendEndpoints.has(key)) {
      addFinding('Allowlist', `${key} is allowlisted for frontend-backend but not used by frontend`);
    }
    const planned = plannedMap.get(key);
    if (!planned) {
      addFinding('Allowlist', `${key} is allowlisted but not listed in planned API section of ${contractsRel}`);
    } else if (entry.beads_id && planned.beadsId && entry.beads_id !== planned.beadsId) {
      addFinding('Allowlist', `${key} allowlist beads_id (${entry.beads_id}) does not match planned API beads ID (${planned.beadsId})`);
    }
  }
  if (entry.beads_id) {
    const status = beadsStatuses.get(entry.beads_id);
    if (!status) {
      addFinding('Allowlist', `${key} references unknown Beads issue: ${entry.beads_id}`);
    } else if (!allowedBeadsStatuses.has(status)) {
      addFinding('Allowlist', `${key} references non-active Beads issue ${entry.beads_id} (status=${status})`);
    }
  }
}

if (findings.length > 0) {
  console.error('Cross consistency check FAILED.');
  if (baseline) {
    console.error(`- baseline: ${baseline}`);
  }
  for (const finding of findings) {
    console.error(`- ${finding}`);
  }
  process.exit(1);
}

console.log('Cross consistency check PASSED.');
if (baseline) {
  console.log(`- baseline: ${baseline}`);
}
console.log(`- backend endpoints: ${backendEndpoints.size}`);
console.log(`- frontend endpoints: ${frontendEndpoints.size}`);
console.log(`- memory implemented endpoints: ${implementedRows.length}`);
console.log(`- memory planned endpoints: ${plannedRows.length}`);
console.log(`- allowlist entries: ${allowlist.size}`);
console.log(`- openapi endpoints: ${openApiEndpoints.size}`);
console.log(`- openapi paths: ${openApiPaths.size}`);
console.log(`- frontend schema paths: ${schemaPaths.size}`);
