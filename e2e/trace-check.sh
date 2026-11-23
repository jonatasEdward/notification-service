#!/usr/bin/env bash
# Looks in Tempo for a trace that starts in the Mão Amiga API and reaches this service through Kafka,
# then prints its spans. Fails if the context was lost anywhere along the way (for example, at the outbox).
set -euo pipefail

TEMPO="${TEMPO_URL:-http://localhost:3200}"
OUTPUT="${1:-trace.md}"
QUERY='{resource.service.name="notification-service"}'

trace_id=""
for _ in $(seq 1 40); do
  trace_id="$(curl -sS -G "$TEMPO/api/search" --data-urlencode "q=$QUERY" --data-urlencode "limit=100" \
    | jq -r '[.traces[]? | select(.rootServiceName == "mao-amiga-api")][0].traceID // empty')"
  [ -n "$trace_id" ] && break
  sleep 3
done

if [ -z "$trace_id" ]; then
  echo "no trace found that starts in mao-amiga-api and reaches notification-service" >&2
  exit 1
fi

trace="$(curl -sS "$TEMPO/api/traces/$trace_id")"
services="$(echo "$trace" | jq -r '[.batches[].resource.attributes[] | select(.key == "service.name") | .value.stringValue] | unique | join(", ")')"

{
  echo "### One trace across both services"
  echo
  echo "Trace \`$trace_id\` covers: $services"
  echo
  echo "| Service | Span |"
  echo "| --- | --- |"
  echo "$trace" | jq -r '
    .batches[]
    | (.resource.attributes[] | select(.key == "service.name") | .value.stringValue) as $service
    | .scopeSpans[].spans[]
    | [(.startTimeUnixNano | tonumber), $service, .name]
    | @tsv' \
    | sort -n | cut -f2- | awk -F'\t' '{ printf "| %s | %s |\n", $1, $2 }'
} > "$OUTPUT"

cat "$OUTPUT"
echo "$services" | grep -q "mao-amiga-api" && echo "$services" | grep -q "notification-service"
