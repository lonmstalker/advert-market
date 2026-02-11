#!/usr/bin/env bash
set -euo pipefail

# Usage: ./switch-color.sh blue|green
# Switches active upstream in nginx to the specified color.

DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
NGINX_DIR="${DEPLOY_DIR}/nginx"
TARGET_COLOR="${1:-}"

if [[ "$TARGET_COLOR" != "blue" && "$TARGET_COLOR" != "green" ]]; then
    echo "Usage: $0 <blue|green>"
    echo "Current active:"
    head -1 "${NGINX_DIR}/upstream-active.conf"
    exit 1
fi

echo "==> Switching to ${TARGET_COLOR}..."

# 1. Verify target is healthy
CONTAINER="am-app-${TARGET_COLOR}"
if ! docker exec "${CONTAINER}" curl -sf http://localhost:8080/actuator/health/readiness > /dev/null 2>&1; then
    echo "ERROR: ${CONTAINER} is not healthy (readiness check failed). Aborting."
    exit 1
fi

# 2. Copy upstream config
cp "${NGINX_DIR}/upstream-${TARGET_COLOR}.conf" "${NGINX_DIR}/upstream-active.conf"

# 3. Reload nginx (graceful — no dropped connections)
docker exec am-nginx nginx -s reload

echo "==> Traffic now goes to: ${TARGET_COLOR}"
echo "==> To rollback: $0 $([ "$TARGET_COLOR" = "blue" ] && echo "green" || echo "blue")"