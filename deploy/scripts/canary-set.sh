#!/usr/bin/env bash
set -euo pipefail

# Set canary percentage via admin API.
# Usage: ./canary-set.sh <percent> [salt]
# Example: ./canary-set.sh 5
# Example: ./canary-set.sh 10 "v2-rollout"

PERCENT="${1:?Usage: $0 <percent> [salt]}"
SALT="${2:-}"
TOKEN="${CANARY_ADMIN_TOKEN:?Set CANARY_ADMIN_TOKEN env var}"
BASE_URL="${CANARY_BASE_URL:-http://localhost:8080}"

echo "==> Setting canary to ${PERCENT}%..."

BODY="{\"percent\": ${PERCENT}"
if [[ -n "$SALT" ]]; then
    BODY="${BODY}, \"salt\": \"${SALT}\""
fi
BODY="${BODY}}"

RESPONSE=$(curl -sf -X PUT \
    "${BASE_URL}/internal/v1/canary" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${BODY}")

echo "==> Response: ${RESPONSE}"