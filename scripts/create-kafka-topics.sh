#!/usr/bin/env bash
set -euo pipefail

KAFKA_CONTAINER="${KAFKA_CONTAINER:-am-kafka}"
BOOTSTRAP="${BOOTSTRAP_SERVER:-localhost:9092}"

create_topic() {
    local topic="$1"
    local partitions="$2"
    docker exec "$KAFKA_CONTAINER" \
        /opt/kafka/bin/kafka-topics.sh \
        --create \
        --if-not-exists \
        --bootstrap-server "$BOOTSTRAP" \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor 1
    echo "Created topic: $topic ($partitions partitions)"
}

echo "Creating Kafka topics..."

create_topic "deal.events"                6
create_topic "escrow.commands"            3
create_topic "escrow.confirmations"       3
create_topic "delivery.commands"          3
create_topic "delivery.results"           3
create_topic "notifications.outbox"       3
create_topic "reconciliation.triggers"    1
create_topic "deal.deadlines"             3

echo "All topics created."
