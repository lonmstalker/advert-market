#!/usr/bin/env bash
set -e -u -o pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_SRC="$REPO_ROOT/scripts/hooks"
HOOKS_DST="$REPO_ROOT/.git/hooks"

for hook in "$HOOKS_SRC"/*; do
  name="$(basename "$hook")"
  target="$HOOKS_DST/$name"

  if [ -e "$target" ] && [ ! -L "$target" ]; then
    echo "skip: .git/hooks/$name already exists (not a symlink)"
    continue
  fi

  ln -sf "$hook" "$target"
  chmod +x "$target"
  echo "installed: .git/hooks/$name -> scripts/hooks/$name"
done
