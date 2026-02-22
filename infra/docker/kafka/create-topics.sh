#!/usr/bin/env sh
# create-topics.sh (line-by-line topics)
# Idempotent topic creation script.
# Reads topics from /topics.txt (one topic per line). Lines starting with # or empty lines are ignored.
# Uses the exact command form (no partitions/replication):
#   ./kafka-topics.sh --bootstrap-server <host> --create --topic <topic-name>

set -u
BOOTSTRAP_SERVER=${BOOTSTRAP_SERVER:-broker:9092}
TOPICS_FILE=/topics.txt
MAX_WAIT_SECONDS=${MAX_WAIT_SECONDS:-180}
SLEEP_SECONDS=${SLEEP_SECONDS:-2}

# Find kafka-topics command
find_kafka_topics() {
  if command -v kafka-topics.sh >/dev/null 2>&1; then
    echo "kafka-topics.sh"
    return 0
  fi
  for p in /bin/kafka-topics.sh /opt/kafka/bin/kafka-topics.sh /usr/bin/kafka-topics.sh /opt/bitnami/kafka/bin/kafka-topics.sh; do
    if [ -x "$p" ]; then
      echo "$p"
      return 0
    fi
  done
  return 1
}

KAFKA_TOPICS_CMD=$(find_kafka_topics) || true
if [ -z "$KAFKA_TOPICS_CMD" ]; then
  echo "ERROR: kafka-topics.sh not found in PATH or known locations."
  ls -la /bin || true
  ls -la /opt || true
  exit 2
fi

echo "Using kafka-topics command: $KAFKA_TOPICS_CMD"

# Require topics file only
if [ ! -f "$TOPICS_FILE" ]; then
  echo "No topics file found at $TOPICS_FILE; nothing to do."
  exit 0
fi

# Read topics one-per-line, ignore empty lines and comments
topics_list=""
while IFS= read -r line || [ -n "$line" ]; do
  # remove CR for Windows-formatted files
  line=$(echo "$line" | tr -d '\r')
  # trim leading/trailing whitespace
  topic=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  # skip empty or comment lines
  case "$topic" in
    ""|\#*) continue ;;
  esac
  # accumulate
  topics_list="$topics_list $topic"
done < "$TOPICS_FILE"

if [ -z "$(echo "$topics_list" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')" ]; then
  echo "No topics found in $TOPICS_FILE; nothing to do."
  exit 0
fi

# Wait for broker readiness
echo "Waiting for Kafka broker at $BOOTSTRAP_SERVER to be ready (timeout ${MAX_WAIT_SECONDS}s)..."
elapsed=0
until $KAFKA_TOPICS_CMD --bootstrap-server "$BOOTSTRAP_SERVER" --list >/dev/null 2>&1; do
  if [ "$elapsed" -ge "$MAX_WAIT_SECONDS" ]; then
    echo "ERROR: broker did not become ready within ${MAX_WAIT_SECONDS}s"
    exit 3
  fi
  sleep $SLEEP_SECONDS
  elapsed=$((elapsed + SLEEP_SECONDS))
done

echo "Broker is ready. Creating topics..."

# Iterate topics (space-separated list)
for topic in $topics_list; do
  # Ensure topic is trimmed
  topic=$(echo "$topic" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  if [ -z "$topic" ]; then
    continue
  fi

  # Check if exists
  if $KAFKA_TOPICS_CMD --bootstrap-server "$BOOTSTRAP_SERVER" --list | grep -xq "$topic"; then
    echo "Topic '$topic' already exists; skipping"
    continue
  fi

  echo "Creating topic '$topic'"
  $KAFKA_TOPICS_CMD --bootstrap-server "$BOOTSTRAP_SERVER" --create --topic "$topic" || {
    echo "Create failed for $topic; retrying once..."
    sleep 1
    $KAFKA_TOPICS_CMD --bootstrap-server "$BOOTSTRAP_SERVER" --create --topic "$topic" || {
      echo "ERROR: Retry failed for $topic"
      exit 4
    }
  }
  echo "Created '$topic'"
done

echo "All requested topics processed."
exit 0
