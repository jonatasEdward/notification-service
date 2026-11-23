#!/usr/bin/env bash
# Waits until every donation registered during the load test has become a notification, then writes a
# Markdown report combining k6 (producer side) and the consumer's own latency metric.
#   usage: report.sh <k6-summary.json> <processed count before the test> <output.md>
set -euo pipefail

SUMMARY="$1"
BASELINE="$2"
OUTPUT="$3"
PROMETHEUS="${NOTIFICATIONS_URL:-http://localhost:8082}/actuator/prometheus"

metric() { curl -sS "$PROMETHEUS" | awk -v m="$1" '$1 == m { print $2 }' | head -1; }
quantile() { curl -sS "$PROMETHEUS" | grep '^notifications_event_latency_seconds{' | grep "quantile=\"$1\"" | awk '{ print $2 }' | head -1; }
ms() { awk -v s="$1" 'BEGIN { printf "%.0f", s * 1000 }'; }

registered="$(jq '.metrics.donations_registered.values.count // 0' "$SUMMARY")"
expected=$((BASELINE + registered))

drain_start="$(date +%s)"
processed=0
for _ in $(seq 1 120); do
  processed="$(metric notifications_event_latency_seconds_count | cut -d. -f1)"
  [ "${processed:-0}" -ge "$expected" ] && break
  sleep 1
done
drain_seconds=$(( $(date +%s) - drain_start ))

http='.metrics["http_req_duration{name:register_donation}"].values'
{
  echo "## Load test results"
  echo
  echo "Runner: $(nproc) vCPU, $(free -g | awk '/^Mem:/ { print $2 }') GB RAM, all services and dependencies in Docker on the same machine."
  echo
  echo "### Producer side (k6 against the Mão Amiga API)"
  echo
  echo "| Metric | Value |"
  echo "| --- | --- |"
  echo "| Donations registered | $registered |"
  echo "| Throughput | $(jq -r '.metrics.http_reqs.values.rate | floor' "$SUMMARY") req/s |"
  echo "| Failed requests | $(jq -r '(.metrics.http_req_failed.values.rate * 100 * 100 | floor) / 100' "$SUMMARY")% |"
  echo "| HTTP latency p50 / p95 / p99 | $(jq -r "$http.med | floor" "$SUMMARY") / $(jq -r "$http[\"p(95)\"] | floor" "$SUMMARY") / $(jq -r "$http[\"p(99)\"] | floor" "$SUMMARY") ms |"
  echo
  echo "### Consumer side (event created -> notification stored)"
  echo
  echo "| Metric | Value |"
  echo "| --- | --- |"
  echo "| Notifications stored | $(( ${processed:-0} - BASELINE )) of $registered |"
  echo "| End-to-end latency p50 / p95 / p99 | $(ms "$(quantile 0.5)") / $(ms "$(quantile 0.95)") / $(ms "$(quantile 0.99)") ms |"
  echo "| Max end-to-end latency | $(ms "$(metric notifications_event_latency_seconds_max)") ms |"
  echo "| Time to drain after the load stopped | ${drain_seconds} s |"
  echo "| Duplicates skipped | $(metric notifications_events_duplicate_total | cut -d. -f1) |"
} > "$OUTPUT"

cat "$OUTPUT"
[ "${processed:-0}" -ge "$expected" ] || { echo "not every donation became a notification" >&2; exit 1; }
