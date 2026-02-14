#!/usr/bin/env bash
set -euo pipefail

# One-command production deploy:
# 1) Run local quality gates (backend + frontend + consistency checks)
# 2) Build artifacts (bootJar + frontend dist + docker image)
# 3) Push git
# 4) Upload artifacts to the server (jar + dist)
# 5) Build image on the server and run blue/green switch
#
# Requirements (local):
# - git, node, npm, docker, rsync, ssh, ./gradlew
#
# Requirements (server):
# - docker + docker compose v2
# - a clone of this repo at $DEPLOY_DIR
# - deploy/.env present (see deploy/README.md)
#
# Usage:
#   DEPLOY_SSH=user@host DEPLOY_DIR=/home/ad-marketplace/advert-market ./scripts/deploy-prod.sh
#
# Options:
#   --tag <image-tag>     Override docker tag (default: advertmarket:git-<shortsha>)
#   --ssh <user@host>     Override DEPLOY_SSH
#   --dir <path>          Override DEPLOY_DIR
#   --domain <domain>     Domain for informational output / optional checks (default: teleinsight.in)
#   --no-checks           Skip local checks/builds (not recommended)
#   --no-remote           Skip remote deploy (checks + git push only)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

usage() {
  cat <<'USAGE'
Production deploy helper.

Examples:
  DEPLOY_SSH=ad-marketplace@teleinsight.in DEPLOY_DIR=/home/ad-marketplace/advert-market ./scripts/deploy-prod.sh
  ./scripts/deploy-prod.sh --tag advertmarket:v0.2.0 --ssh user@host --dir /srv/advert-market

Notes:
  - This script deploys from the current local git HEAD (must be on main and clean).
  - It uploads built artifacts (bootJar + frontend dist) and runs blue/green deploy on the server.
USAGE
}

DEPLOY_SSH="${DEPLOY_SSH:-}"
DEPLOY_DIR="${DEPLOY_DIR:-/home/ad-marketplace/advert-market}"
DEPLOY_DOMAIN="${DEPLOY_DOMAIN:-teleinsight.in}"
IMAGE_TAG="${IMAGE_TAG:-}"
NO_CHECKS=0
NO_REMOTE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --tag)
      IMAGE_TAG="${2:?--tag requires a value}"
      shift 2
      ;;
    --ssh)
      DEPLOY_SSH="${2:?--ssh requires a value}"
      shift 2
      ;;
    --dir)
      DEPLOY_DIR="${2:?--dir requires a value}"
      shift 2
      ;;
    --domain)
      DEPLOY_DOMAIN="${2:?--domain requires a value}"
      shift 2
      ;;
    --no-checks)
      NO_CHECKS=1
      shift
      ;;
    --no-remote)
      NO_REMOTE=1
      shift
      ;;
    *)
      echo "ERROR: unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

require_cmd() {
  local cmd="$1"
  command -v "${cmd}" >/dev/null 2>&1 || {
    echo "ERROR: missing required command: ${cmd}" >&2
    exit 1
  }
}

require_repo_clean_on_main() {
  local branch
  branch="$(git rev-parse --abbrev-ref HEAD)"
  if [[ "${branch}" != "main" ]]; then
    echo "ERROR: deploy is allowed only from main (current: ${branch})" >&2
    exit 1
  fi
  if [[ -n "$(git status --porcelain)" ]]; then
    echo "ERROR: working tree is not clean. Commit/stash changes first." >&2
    git status --porcelain >&2
    exit 1
  fi
}

select_boot_jar() {
  local jars
  jars="$(ls -1 advert-market-app/build/libs/advert-market-app-*.jar 2>/dev/null | grep -v -- '-plain.jar' || true)"
  local count
  count="$(echo "${jars}" | sed '/^$/d' | wc -l | tr -d ' ')"
  if [[ "${count}" != "1" ]]; then
    echo "ERROR: expected exactly 1 boot jar under advert-market-app/build/libs/, got ${count}." >&2
    echo "Hint: run ./gradlew :advert-market-app:bootJar and ensure only one non-plain jar exists." >&2
    echo "${jars}" >&2
    exit 1
  fi
  echo "${jars}" | head -n 1
}

echo "==> Preflight (repo)"
require_cmd git
require_repo_clean_on_main

SHORT_SHA="$(git rev-parse --short HEAD)"
if [[ -z "${IMAGE_TAG}" ]]; then
  IMAGE_TAG="advertmarket:git-${SHORT_SHA}"
fi

echo "==> Image tag: ${IMAGE_TAG}"

if [[ "${NO_CHECKS}" -eq 0 ]]; then
  echo "==> Local checks (memory bank / cross-consistency)"
  require_cmd node
  node scripts/check-memory-bank-consistency.mjs
  node scripts/check-cross-consistency.mjs --baseline=HEAD

  echo "==> Local checks (backend)"
  require_cmd ./gradlew
  ./gradlew check
  mkdir -p advert-market-app/build/libs
  rm -f advert-market-app/build/libs/advert-market-app-*.jar
  ./gradlew :advert-market-app:bootJar

  echo "==> Local checks (frontend)"
  require_cmd npm
  pushd advert-market-frontend >/dev/null
  if [[ ! -d node_modules ]]; then
    npm ci
  fi
  npm test
  npm run lint
  npm run build
  popd >/dev/null

  echo "==> Local check (docker build)"
  require_cmd docker
  docker build -t "${IMAGE_TAG}" .
else
  echo "==> Skipping local checks/builds (--no-checks)"
fi

echo "==> Git push"
git push

if [[ "${NO_REMOTE}" -eq 1 ]]; then
  echo "==> Skipping remote deploy (--no-remote). Done."
  exit 0
fi

if [[ -z "${DEPLOY_SSH}" ]]; then
  echo "ERROR: DEPLOY_SSH is not set. Provide via env or --ssh." >&2
  echo "Example: DEPLOY_SSH=ad-marketplace@teleinsight.in DEPLOY_DIR=/home/ad-marketplace/advert-market ./scripts/deploy-prod.sh" >&2
  exit 1
fi

echo "==> Remote: ${DEPLOY_SSH}:${DEPLOY_DIR}"
echo "==> Domain: ${DEPLOY_DOMAIN}"

require_cmd ssh
require_cmd rsync

echo "==> Remote update (git pull with autostash to preserve runtime nginx state files)"
ssh "${DEPLOY_SSH}" "set -euo pipefail; cd '${DEPLOY_DIR}'; git fetch origin; git checkout main; git pull --rebase --autostash origin main"

if [[ "${NO_CHECKS}" -eq 0 ]]; then
  BOOT_JAR="$(select_boot_jar)"
else
  # In --no-checks mode, we still need artifacts.
  echo "==> Building artifacts locally (required even with --no-checks)"
  require_cmd ./gradlew
  mkdir -p advert-market-app/build/libs
  rm -f advert-market-app/build/libs/advert-market-app-*.jar
  ./gradlew :advert-market-app:bootJar
  require_cmd npm
  pushd advert-market-frontend >/dev/null
  if [[ ! -d node_modules ]]; then
    npm ci
  fi
  npm run build
  popd >/dev/null
  BOOT_JAR="$(select_boot_jar)"
fi

echo "==> Upload boot jar"
ssh "${DEPLOY_SSH}" "set -euo pipefail; mkdir -p '${DEPLOY_DIR}/advert-market-app/build/libs'; rm -f '${DEPLOY_DIR}/advert-market-app/build/libs/advert-market-app-'*'.jar'"
rsync -az "${BOOT_JAR}" "${DEPLOY_SSH}:${DEPLOY_DIR}/advert-market-app/build/libs/advert-market-app-deploy.jar"

echo "==> Upload frontend dist"
if [[ ! -d advert-market-frontend/dist ]]; then
  echo "ERROR: advert-market-frontend/dist not found. Build the frontend first." >&2
  exit 1
fi
ssh "${DEPLOY_SSH}" "set -euo pipefail; mkdir -p '${DEPLOY_DIR}/deploy/nginx/html'"
rsync -az --delete --exclude '.gitkeep' "advert-market-frontend/dist/" "${DEPLOY_SSH}:${DEPLOY_DIR}/deploy/nginx/html/"

echo "==> Remote build + blue/green deploy"
ssh "${DEPLOY_SSH}" "set -euo pipefail;
  cd '${DEPLOY_DIR}';
  docker build -t '${IMAGE_TAG}' .;
  cd deploy;
  ./scripts/deploy-blue-green.sh '${IMAGE_TAG}';
  echo '==> Smoke: nginx health';
  curl -fsS http://127.0.0.1/health >/dev/null;
  echo '==> Smoke: auth endpoint returns localized ProblemDetail (no i18n keys)';
  active=\$(grep -o 'am-app-[a-z]*' nginx/upstream-active.conf | head -1 | sed 's/am-app-//');
  out=\$(docker exec \"am-app-\${active}\" sh -lc \"curl -sS -H 'Content-Type: application/json' -d '{\\\\\\\"initData\\\\\\\":\\\\\\\"\\\\\\\"}' -w '\\\\n%{http_code}\\\\n' http://localhost:8080/api/v1/auth/login\");
  code=\$(echo \"\${out}\" | tail -n 1);
  body=\$(echo \"\${out}\" | sed '\$d');
  if [[ \"\${code}\" != \"400\" ]]; then
    echo \"ERROR: expected 400 from /api/v1/auth/login for empty initData, got \${code}\";
    echo \"\${body}\";
    exit 1;
  fi;
  if echo \"\${body}\" | grep -q '\"error\\.'; then
    echo 'ERROR: localization keys leaked into API response (expected resolved messages).';
    echo \"\${body}\";
    exit 1;
  fi;
  echo '==> Smoke OK';
"

echo "==> Done: ${DEPLOY_DOMAIN} (image: ${IMAGE_TAG})"
