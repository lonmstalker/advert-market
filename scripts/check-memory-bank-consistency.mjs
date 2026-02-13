#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const repoRoot = process.cwd();
const configPath = path.resolve(repoRoot, "scripts/memory-bank-consistency.config.json");

if (!fs.existsSync(configPath)) {
  console.error(`Config not found: ${configPath}`);
  process.exit(1);
}

const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
const memoryBankRoot = path.resolve(repoRoot, config.memoryBankRoot ?? ".memory-bank");
const rootIndexRel = config.rootIndex ?? "00-index.md";
const entryIndexRel = config.entryIndex ?? "index.md";
const rootIndexPath = path.resolve(memoryBankRoot, rootIndexRel);

const failures = [];

function relFromRoot(absPath) {
  return path.relative(memoryBankRoot, absPath).replaceAll(path.sep, "/");
}

function addFailure(check, message) {
  failures.push(`[${check}] ${message}`);
}

function readText(absPath) {
  return fs.readFileSync(absPath, "utf8");
}

function walkMarkdownFiles(dirAbs) {
  const out = [];
  const entries = fs.readdirSync(dirAbs, { withFileTypes: true });
  for (const entry of entries) {
    const abs = path.join(dirAbs, entry.name);
    if (entry.isDirectory()) {
      out.push(...walkMarkdownFiles(abs));
      continue;
    }
    if (entry.isFile() && entry.name.endsWith(".md")) {
      out.push(abs);
    }
  }
  return out;
}

function isExternalLink(target) {
  return /^(https?:\/\/|mailto:|tel:)/i.test(target);
}

function extractMarkdownLinks(content) {
  const links = [];
  const regex = /\[[^\]]*]\(([^)]+)\)/g;
  let match;
  while ((match = regex.exec(content)) !== null) {
    const rawTarget = match[1].trim();
    if (!rawTarget) {
      continue;
    }
    links.push(rawTarget);
  }
  return links;
}

function splitTarget(target) {
  const [pathAndQuery, anchor = ""] = target.split("#");
  const [cleanPath] = pathAndQuery.split("?");
  return {
    pathPart: cleanPath,
    anchorPart: anchor ? decodeURIComponent(anchor) : "",
  };
}

function slugifyHeading(text) {
  return text
    .trim()
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^\p{Letter}\p{Number}\s-]/gu, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}

function collectHeadingSlugs(content) {
  const slugCounts = new Map();
  const slugs = new Set();
  const lines = content.split(/\r?\n/u);
  for (const line of lines) {
    const match = line.match(/^#{1,6}\s+(.+?)\s*$/u);
    if (!match) {
      continue;
    }
    const base = slugifyHeading(match[1]);
    if (!base) {
      continue;
    }
    const count = slugCounts.get(base) ?? 0;
    slugCounts.set(base, count + 1);
    const slug = count === 0 ? base : `${base}-${count}`;
    slugs.add(slug);
  }
  return slugs;
}

function toResolvedTarget(sourceFileAbs, target) {
  const { pathPart, anchorPart } = splitTarget(target);
  if (!pathPart) {
    return { resolvedPath: sourceFileAbs, anchorPart };
  }
  if (path.isAbsolute(pathPart)) {
    return { resolvedPath: path.normalize(pathPart), anchorPart };
  }
  return {
    resolvedPath: path.normalize(path.resolve(path.dirname(sourceFileAbs), pathPart)),
    anchorPart,
  };
}

const markdownFiles = walkMarkdownFiles(memoryBankRoot).sort();
if (markdownFiles.length === 0) {
  addFailure("Setup", `No markdown files found under ${memoryBankRoot}`);
}
if (!fs.existsSync(rootIndexPath)) {
  addFailure("Setup", `Root index not found: ${rootIndexPath}`);
}

const markdownSet = new Set(markdownFiles);
const headingMap = new Map();
for (const file of markdownFiles) {
  headingMap.set(file, collectHeadingSlugs(readText(file)));
}

// Check 1: Link integrity (paths + anchors)
for (const sourceAbs of markdownFiles) {
  const sourceRel = relFromRoot(sourceAbs);
  const content = readText(sourceAbs);
  const links = extractMarkdownLinks(content);
  for (const target of links) {
    if (isExternalLink(target) || target.startsWith("#")) {
      continue;
    }
    const { resolvedPath, anchorPart } = toResolvedTarget(sourceAbs, target);
    if (!fs.existsSync(resolvedPath)) {
      addFailure("LinkIntegrity", `${sourceRel} -> missing target: ${target}`);
      continue;
    }
    if (
      anchorPart &&
      resolvedPath.endsWith(".md") &&
      markdownSet.has(resolvedPath)
    ) {
      const slugs = headingMap.get(resolvedPath) ?? new Set();
      if (!slugs.has(anchorPart.toLowerCase())) {
        addFailure(
          "LinkIntegrity",
          `${sourceRel} -> invalid anchor #${anchorPart} in ${relFromRoot(resolvedPath)}`,
        );
      }
    }
  }
}

// Build inbound map for local markdown links and index completeness
const inboundLinks = new Map(markdownFiles.map((file) => [file, 0]));
const rootIndexLinks = new Set();
const rootIndexContent = fs.existsSync(rootIndexPath) ? readText(rootIndexPath) : "";

for (const sourceAbs of markdownFiles) {
  const links = extractMarkdownLinks(readText(sourceAbs));
  for (const target of links) {
    if (isExternalLink(target) || target.startsWith("#")) {
      continue;
    }
    const { resolvedPath } = toResolvedTarget(sourceAbs, target);
    if (!markdownSet.has(resolvedPath)) {
      continue;
    }
    inboundLinks.set(resolvedPath, (inboundLinks.get(resolvedPath) ?? 0) + 1);
    if (sourceAbs === rootIndexPath) {
      rootIndexLinks.add(resolvedPath);
    }
  }
}

// Check 2: Index completeness
const excludeFromIndex = new Set(config.excludeFromIndex ?? []);
for (const fileAbs of markdownFiles) {
  const rel = relFromRoot(fileAbs);
  if (rel === rootIndexRel || excludeFromIndex.has(rel)) {
    continue;
  }
  if (!rootIndexLinks.has(fileAbs)) {
    addFailure("IndexCompleteness", `${rel} is missing from ${rootIndexRel}`);
  }
}

// Check 3: No orphans
const orphanExclusions = new Set(config.orphanExclusions ?? []);
for (const [fileAbs, inbound] of inboundLinks.entries()) {
  const rel = relFromRoot(fileAbs);
  if (orphanExclusions.has(rel)) {
    continue;
  }
  if (inbound === 0) {
    addFailure("NoOrphans", `${rel} has no inbound links`);
  }
}

// Check 4: Invariant consistency
const specsDir = path.resolve(memoryBankRoot, "14-implementation-specs");
const specFiles = fs.existsSync(specsDir) ? walkMarkdownFiles(specsDir).sort() : [];
const indexSpecLinks = [...rootIndexLinks].filter((file) =>
  file.startsWith(specsDir + path.sep),
);

if (specFiles.length !== indexSpecLinks.length) {
  addFailure(
    "Invariant",
    `implementation specs count mismatch: filesystem=${specFiles.length}, root index links=${indexSpecLinks.length}`,
  );
}
for (const specAbs of specFiles) {
  if (!rootIndexLinks.has(specAbs)) {
    addFailure("Invariant", `spec missing from root index: ${relFromRoot(specAbs)}`);
  }
}

const footerMatch = rootIndexContent.match(/(\d+)\s+files total\./u);
if (!footerMatch) {
  addFailure("Invariant", `${rootIndexRel} is missing 'files total' footer`);
} else {
  const excludeFromFooterCount = new Set(config.excludeFromFooterCount ?? []);
  const expectedCount = markdownFiles.filter(
    (fileAbs) => !excludeFromFooterCount.has(relFromRoot(fileAbs)),
  ).length;
  const footerCount = Number.parseInt(footerMatch[1], 10);
  if (footerCount !== expectedCount) {
    addFailure(
      "Invariant",
      `footer count mismatch in ${rootIndexRel}: footer=${footerCount}, expected=${expectedCount}`,
    );
  }
}

for (const rule of config.requiredPhrases ?? []) {
  const fileAbs = path.resolve(memoryBankRoot, rule.file);
  if (!fs.existsSync(fileAbs)) {
    addFailure("Invariant", `required phrase file missing: ${rule.file}`);
    continue;
  }
  const content = readText(fileAbs);
  if (!content.includes(rule.pattern)) {
    addFailure("Invariant", `${rule.file} is missing required phrase: "${rule.pattern}"`);
  }
}

for (const rule of config.forbiddenPhrases ?? []) {
  const fileAbs = path.resolve(memoryBankRoot, rule.file);
  if (!fs.existsSync(fileAbs)) {
    addFailure("Invariant", `forbidden phrase file missing: ${rule.file}`);
    continue;
  }
  const content = readText(fileAbs);
  if (content.includes(rule.pattern)) {
    addFailure("Invariant", `${rule.file} still contains forbidden phrase: "${rule.pattern}"`);
  }
}

// Check 5: Language policy
const languageMode = config.language?.mode ?? "english";
if (!["english", "bilingual"].includes(languageMode)) {
  addFailure("LanguagePolicy", `Unsupported language mode: ${languageMode}`);
}
if (languageMode === "english") {
  const cyrillicRegex = /[А-Яа-яЁё]/u;
  for (const fileAbs of markdownFiles) {
    const rel = relFromRoot(fileAbs);
    if (cyrillicRegex.test(readText(fileAbs))) {
      addFailure("LanguagePolicy", `${rel} contains Cyrillic characters`);
    }
  }
}

// Frontmatter requirement for selected files
for (const rel of config.requiredFrontmatter ?? []) {
  const abs = path.resolve(memoryBankRoot, rel);
  if (!fs.existsSync(abs)) {
    addFailure("Frontmatter", `required file missing: ${rel}`);
    continue;
  }
  const content = readText(abs);
  if (!content.startsWith("---\n")) {
    addFailure("Frontmatter", `${rel} must start with YAML frontmatter`);
  }
}

if (failures.length > 0) {
  console.error("Memory bank consistency check FAILED.");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

const excludeFromFooterCount = new Set(config.excludeFromFooterCount ?? []);
const countedDocs = markdownFiles.filter(
  (fileAbs) => !excludeFromFooterCount.has(relFromRoot(fileAbs)),
).length;

console.log("Memory bank consistency check PASSED.");
console.log(`- markdown files: ${markdownFiles.length}`);
console.log(`- footer-counted files: ${countedDocs}`);
console.log(`- implementation specs: ${specFiles.length}`);
console.log(`- language mode: ${languageMode}`);
