// Load test: donations are registered in the Mão Amiga API at a constant rate. Each one goes through the
// outbox and Kafka and must become a notification here. k6 measures the HTTP side; report.sh reads the
// consumer's metrics to measure how long events took to become notifications.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const API = __ENV.API_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 25);
const DURATION = __ENV.DURATION || '60s';
const DONORS = 20;

export const options = {
  scenarios: {
    donations: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 30,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:register_donation}': ['p(95)<500'],
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

const registered = new Counter('donations_registered');
const json = { 'Content-Type': 'application/json' };

function signUp(name, role) {
  const email = `${name}-${Date.now()}-${Math.random().toString(36).slice(2)}@load.dev`;
  const response = http.post(`${API}/api/v1/auth/cadastro`,
    JSON.stringify({ nome: name, email, senha: 'load-test-password', papel: role }), { headers: json });
  return response.json('token');
}

export function setup() {
  const responsible = signUp('responsible', 'RESPONSAVEL');
  const point = http.post(`${API}/api/v1/pontos`, JSON.stringify({
    nome: 'Ponto de Carga', endereco: 'Rua 1', bairro: 'Centro', cidade: 'Goiânia',
    diasHorarios: 'Todos os dias', recebe: 'Alimentos', distribui: null,
  }), { headers: { ...json, Authorization: `Bearer ${responsible}` } }).json('id');
  const donors = Array.from({ length: DONORS }, (_, i) => signUp(`donor${i}`, 'DOADOR'));
  return { point, donors };
}

export default function (data) {
  const token = data.donors[Math.floor(Math.random() * data.donors.length)];
  const response = http.post(`${API}/api/v1/doacoes`,
    JSON.stringify({ pontoId: data.point, tipoItem: 'Arroz 5kg', quantidade: 1 }),
    { headers: { ...json, Authorization: `Bearer ${token}` }, tags: { name: 'register_donation' } });
  if (check(response, { 'donation registered (201)': (r) => r.status === 201 })) {
    registered.add(1);
  }
}

export function handleSummary(data) {
  return { 'k6-summary.json': JSON.stringify(data, null, 2) };
}
