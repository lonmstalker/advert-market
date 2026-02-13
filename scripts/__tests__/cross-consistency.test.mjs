import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "..", "..");
const checkerPath = path.resolve(repoRoot, "scripts/check-cross-consistency.mjs");

describe("cross consistency", () => {
  it("passes repository cross-consistency checks", () => {
    const result = spawnSync("node", [checkerPath, "--baseline=HEAD"], {
      cwd: repoRoot,
      encoding: "utf8",
    });

    assert.equal(
      result.status,
      0,
      `checker failed with code ${result.status}\nstdout:\n${result.stdout}\nstderr:\n${result.stderr}`,
    );
  });
});
