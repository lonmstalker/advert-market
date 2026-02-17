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
#   --env-server <path>   Load server connection vars from a .env-style file (default: .env.server)
#   --no-checks           Skip local checks/builds (not recommended)
#   --no-remote           Skip remote deploy (checks + git push only)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

usage() {
  cat <<'USAGE'
Production deploy helper.

Examples:
  DEPLOY_SSH=ad-marketplace@teleinsight.in DEPLOY_DIR=/home/ad-marketplace/advert-market ./scripts/deploy-prod.sh
  ./scripts/deploy-prod.sh --env-server .env.server
  ./scripts/deploy-prod.sh --tag advertmarket:v0.2.0 --ssh user@host --dir /srv/advert-market

Notes:
  - This script deploys from the current local git HEAD (must be on main and clean).
  - It uploads built artifacts (bootJar + frontend dist) and runs blue/green deploy on the server.
  - Server connection file (.env.server by default) can define:
      SERVER_HOST, SERVER_USER, SSH_KEY (optional), DEPLOY_DIR (optional)
USAGE
}

ENV_SERVER_FILE="${ENV_SERVER_FILE:-.env.server}"

DEPLOY_SSH="${DEPLOY_SSH:-}"
DEPLOY_DIR="${DEPLOY_DIR:-}"
DEPLOY_DOMAIN="${DEPLOY_DOMAIN:-teleinsight.in}"
IMAGE_TAG="${IMAGE_TAG:-}"
NO_CHECKS=0
NO_REMOTE=0

expand_tilde() {
  local raw="$1"
  if [[ "${raw}" == "~" ]]; then
    echo "${HOME}"
    return
  fi
  if [[ "${raw}" == "~/"* ]]; then
    echo "${HOME}/${raw#~/}"
    return
  fi
  echo "${raw}"
}

load_env_server_file() {
  local file="$1"
  if [[ ! -f "${file}" ]]; then
    return 0
  fi

  # .env.server is developer-local. Expect KEY=VALUE format.
  # shellcheck disable=SC1090
  set -a
  source "${file}"
  set +a

  # Derived defaults.
  if [[ -z "${DEPLOY_SSH:-}" && -n "${SERVER_USER:-}" && -n "${SERVER_HOST:-}" ]]; then
    DEPLOY_SSH="${SERVER_USER}@${SERVER_HOST}"
  fi
  if [[ -z "${DEPLOY_DIR:-}" && -n "${SERVER_USER:-}" ]]; then
    DEPLOY_DIR="/home/${SERVER_USER}/advert-market"
  fi

  if [[ -n "${SSH_KEY:-}" && -z "${DEPLOY_SSH_OPTS:-}" ]]; then
    local key
    key="$(expand_tilde "${SSH_KEY}")"
    DEPLOY_SSH_OPTS="-i ${key} -o IdentitiesOnly=yes"
  fi
}

SSH_CMD=(ssh)
RSYNC_SSH="ssh"
DEPLOY_SSH_OPTS="${DEPLOY_SSH_OPTS:-}"
DEPLOY_SSH_CONTROL_PATH="${DEPLOY_SSH_CONTROL_PATH:-}"

# First pass: allow overriding env-server path.
args=("$@")
for ((i=0; i<${#args[@]}; i++)); do
  if [[ "${args[$i]}" == "--env-server" ]]; then
    ENV_SERVER_FILE="${args[$((i+1))]:?--env-server requires a value}"
  fi
done

load_env_server_file "${ENV_SERVER_FILE}"

# SSH in this environment is flaky (port can briefly refuse connections).
# Multiplexing reduces the number of TCP handshakes and keeps a single session
# alive across the whole deploy (including long local build steps).
if [[ "${DEPLOY_SSH_OPTS}" != *"ControlMaster="* ]]; then
  if [[ -z "${DEPLOY_SSH_CONTROL_PATH}" ]]; then
    DEPLOY_SSH_CONTROL_PATH="$(mktemp -u /tmp/am-deploy-ssh-XXXXXX)"
  fi
  DEPLOY_SSH_OPTS="${DEPLOY_SSH_OPTS} -o ControlMaster=auto -o ControlPersist=10m -o ControlPath=${DEPLOY_SSH_CONTROL_PATH}"
fi

if [[ -n "${DEPLOY_SSH_OPTS}" ]]; then
  # Split opts into array safely.
  # shellcheck disable=SC2206
  DEPLOY_SSH_OPTS_ARR=(${DEPLOY_SSH_OPTS})
  SSH_CMD=(ssh "${DEPLOY_SSH_OPTS_ARR[@]}")
  RSYNC_SSH="ssh ${DEPLOY_SSH_OPTS}"
fi

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
    --env-server)
      # Already processed in the first pass. Keep for UX.
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

if [[ -z "${DEPLOY_DIR}" ]]; then
  DEPLOY_DIR="/home/${SERVER_USER:-ad-marketplace}/advert-market"
fi

cleanup_ssh_mux() {
  if [[ -n "${DEPLOY_SSH_CONTROL_PATH:-}" && -n "${DEPLOY_SSH:-}" ]]; then
    "${SSH_CMD[@]}" -O exit "${DEPLOY_SSH}" >/dev/null 2>&1 || true
  fi
}
trap cleanup_ssh_mux EXIT

require_cmd() {
  local cmd="$1"
  command -v "${cmd}" >/dev/null 2>&1 || {
    echo "ERROR: missing required command: ${cmd}" >&2
    exit 1
  }
}

retry() {
  local attempts="${1:?attempts required}"
  local delay_seconds="${2:?delay_seconds required}"
  shift 2

  local i
  for ((i=1; i<=attempts; i++)); do
    if "$@"; then
      return 0
    fi
    if [[ "${i}" -lt "${attempts}" ]]; then
      echo "WARN: command failed (attempt ${i}/${attempts}); retrying in ${delay_seconds}s..." >&2
      sleep "${delay_seconds}"
    fi
  done
  return 1
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

echo "==> Remote connectivity"
retry 10 3 "${SSH_CMD[@]}" "${DEPLOY_SSH}" "true"

echo "==> Remote update"
if retry 3 2 "${SSH_CMD[@]}" "${DEPLOY_SSH}" "test -d '${DEPLOY_DIR}/.git'"; then
  echo "==> Remote is a git repo: git pull --rebase --autostash"
  retry 5 2 "${SSH_CMD[@]}" "${DEPLOY_SSH}" "set -euo pipefail; cd '${DEPLOY_DIR}'; git fetch origin; git checkout main; git pull --rebase --autostash origin main"
else
  echo "==> Remote is NOT a git repo: syncing deploy infra (no .env, no nginx state)"
  retry 5 2 "${SSH_CMD[@]}" "${DEPLOY_SSH}" "set -euo pipefail; mkdir -p '${DEPLOY_DIR}/deploy/scripts' '${DEPLOY_DIR}/deploy/nginx'"
  retry 5 2 "${SSH_CMD[@]}" "${DEPLOY_SSH}" "set -euo pipefail; if [[ -d '${DEPLOY_DIR}/deploy/nginx/canary-api-split.conf' ]]; then rm -rf '${DEPLOY_DIR}/deploy/nginx/canary-api-split.conf'; fi"

  retry 5 2 rsync -az -e "${RSYNC_SSH}" "Dockerfile" "${DEPLOY_SSH}:${DEPLOY_DIR}/Dockerfile"
  if [[ -f .dockerignore ]]; then
    retry 5 2 rsync -az -e "${RSYNC_SSH}" ".dockerignore" "${DEPLOY_SSH}:${DEPLOY_DIR}/.dockerignore"
  fi

  retry 5 2 rsync -az -e "${RSYNC_SSH}" \
    "deploy/README.md" \
    "deploy/RUNBOOK.md" \
    "deploy/docker-compose.prod.yml" \
    "${DEPLOY_SSH}:${DEPLOY_DIR}/deploy/"
  if [[ -f "deploy/docker-compose.server.override.yml" ]]; then
    retry 5 2 rsync -az -e "${RSYNC_SSH}" "deploy/docker-compose.server.override.yml" "${DEPLOY_SSH}:${DEPLOY_DIR}/deploy/"
  fi
  # Use --inplace for bind-mounted nginx config files so running containers
  # observe updates without requiring full recreation.
  retry 5 2 rsync -az --inplace -e "${RSYNC_SSH}" \
    "deploy/nginx/nginx.conf" \
    "deploy/nginx/canary-api.conf" \
    "deploy/nginx/canary-api-split.conf" \
    "deploy/nginx/upstream-blue.conf" \
    "deploy/nginx/upstream-green.conf" \
    "${DEPLOY_SSH}:${DEPLOY_DIR}/deploy/nginx/"
  retry 5 2 rsync -az -e "${RSYNC_SSH}" "deploy/scripts/" "${DEPLOY_SSH}:${DEPLOY_DIR}/deploy/scripts/"
fi

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
retry 10 3 "${SSH_CMD[@]}" "${DEPLOY_SSH}" "set -euo pipefail; mkdir -p '${DEPLOY_DIR}/advert-market-app/build/libs'; rm -f '${DEPLOY_DIR}/advert-market-app/build/libs/advert-market-app-'*'.jar'"
retry 10 3 rsync -az -e "${RSYNC_SSH}" "${BOOT_JAR}" "${DEPLOY_SSH}:${DEPLOY_DIR}/advert-market-app/build/libs/advert-market-app-deploy.jar"

echo "==> Upload frontend dist"
if [[ ! -d advert-market-frontend/dist ]]; then
  echo "ERROR: advert-market-frontend/dist not found. Build the frontend first." >&2
  exit 1
fi
retry 10 3 "${SSH_CMD[@]}" "${DEPLOY_SSH}" "set -euo pipefail; mkdir -p '${DEPLOY_DIR}/deploy/nginx/html'"
retry 10 3 rsync -az -e "${RSYNC_SSH}" --delete --exclude '.gitkeep' "advert-market-frontend/dist/" "${DEPLOY_SSH}:${DEPLOY_DIR}/deploy/nginx/html/"

echo "==> Remote build + blue/green deploy"
"${SSH_CMD[@]}" "${DEPLOY_SSH}" "set -euo pipefail;
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
