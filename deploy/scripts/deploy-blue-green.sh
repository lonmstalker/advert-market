#!/usr/bin/env bash
set -euo pipefail

# Blue-green deployment script.
# Deploys new version to the INACTIVE color, waits for health, then switches.
#
# Usage: ./deploy-blue-green.sh <new-image-tag>
# Example: ./deploy-blue-green.sh advertmarket:v0.2.0

DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
NEW_IMAGE="${1:?Usage: $0 <image-tag>}"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.prod.yml"

# Determine current active color
CURRENT_ACTIVE=$(grep -o 'am-app-[a-z]*' "${DEPLOY_DIR}/nginx/upstream-active.conf" | head -1 | sed 's/am-app-//')
if [[ "$CURRENT_ACTIVE" == "blue" ]]; then
    INACTIVE="green"
else
    INACTIVE="blue"
fi

echo "==> Current active: ${CURRENT_ACTIVE}"
echo "==> Deploying to: ${INACTIVE} with image: ${NEW_IMAGE}"

# 1. Start inactive color with new image
export APP_IMAGE="${NEW_IMAGE}"
docker compose -f "${COMPOSE_FILE}" --profile "${INACTIVE}" up -d "app-${INACTIVE}"

# 2. Wait for readiness
CONTAINER="am-app-${INACTIVE}"
echo "==> Waiting for ${CONTAINER} to become ready..."
MAX_WAIT=120
ELAPSED=0
while ! docker exec "${CONTAINER}" curl -sf http://localhost:8080/actuator/health/readiness > /dev/null 2>&1; do
    sleep 2
    ELAPSED=$((ELAPSED + 2))
    if [[ $ELAPSED -ge $MAX_WAIT ]]; then
        echo "ERROR: ${CONTAINER} did not become ready within ${MAX_WAIT}s. Aborting."
        echo "==> Stopping failed container..."
        docker compose -f "${COMPOSE_FILE}" stop "app-${INACTIVE}"
        exit 1
    fi
done
echo "==> ${CONTAINER} is healthy!"

# 3. Switch traffic
"${DEPLOY_DIR}/scripts/switch-color.sh" "${INACTIVE}"

# 4. Wait for old connections to drain, then stop old color
echo "==> Waiting 30s for old connections to drain..."
sleep 30
docker compose -f "${COMPOSE_FILE}" stop "app-${CURRENT_ACTIVE}"

echo "==> Deployment complete! Active: ${INACTIVE}"
echo "==> Rollback: ${DEPLOY_DIR}/scripts/switch-color.sh ${CURRENT_ACTIVE}"