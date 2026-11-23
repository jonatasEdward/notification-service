#!/usr/bin/env bash
# End-to-end check across both services: a donation registered in the Mão Amiga API must show up as a
# notification for the point's responsible person, and confirming it must notify the donor. This only
# passes if the outbox, Kafka, the consumer and RS256/JWKS token validation all work together.
set -euo pipefail

API="${API_URL:-http://localhost:8080}"
NOTIFICATIONS="${NOTIFICATIONS_URL:-http://localhost:8082}"
RUN_ID="$(date +%s)-$RANDOM"

post() { curl -sS --fail-with-body -X POST "$1" -H 'Content-Type: application/json' ${3:+-H "Authorization: Bearer $3"} -d "$2"; }
get() { curl -sS --fail-with-body "$1" -H "Authorization: Bearer $2"; }

signup() {
  post "$API/api/v1/auth/cadastro" \
    "{\"nome\": \"$1\", \"email\": \"$1-$RUN_ID@e2e.dev\", \"senha\": \"senha-e2e-123\", \"papel\": \"$2\"}" | jq -r .token
}

wait_for_unread() {
  local token="$1" expected="$2" count=0
  for _ in $(seq 1 60); do
    count="$(get "$NOTIFICATIONS/api/v1/notifications/unread-count" "$token" | jq -r .count)"
    [ "$count" -ge "$expected" ] && return 0
    sleep 1
  done
  echo "expected $expected unread notification(s), got $count" >&2
  return 1
}

responsible="$(signup responsible RESPONSAVEL)"
donor="$(signup donor DOADOR)"

point_id="$(post "$API/api/v1/pontos" \
  '{"nome": "Ponto E2E", "endereco": "Rua 1", "bairro": "Centro", "cidade": "Goiânia", "diasHorarios": "Todos os dias", "recebe": "Alimentos", "distribui": null}' \
  "$responsible" | jq -r .id)"

donation_id="$(post "$API/api/v1/doacoes" "{\"pontoId\": \"$point_id\", \"tipoItem\": \"Feijão 1kg\", \"quantidade\": 6}" "$donor" | jq -r .id)"
echo "donation $donation_id registered"

wait_for_unread "$responsible" 1
echo "responsible notified:"
get "$NOTIFICATIONS/api/v1/notifications" "$responsible" | jq -r '.content[0] | "  \(.title): \(.body)"'

post "$API/api/v1/doacoes/$donation_id/confirmar-recebimento" '{}' "$responsible" > /dev/null
wait_for_unread "$donor" 1
echo "donor notified:"
get "$NOTIFICATIONS/api/v1/notifications" "$donor" | jq -r '.content[0] | "  \(.title): \(.body)"'
