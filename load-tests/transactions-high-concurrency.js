import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const acceptedResponses = new Counter('accepted_responses');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PATH = __ENV.API_PATH || '/api/v1/transactions';
const TARGET_URL = `${BASE_URL}${API_PATH}`;

const RATE = Number(__ENV.RATE || 2000);
const DURATION = __ENV.DURATION || '2m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 500);
const MAX_VUS = Number(__ENV.MAX_VUS || 5000);
const SCENARIO_MODE = (__ENV.SCENARIO_MODE || 'all-at-once').toLowerCase();
const BURST_VUS = Number(__ENV.BURST_VUS || 3000);

const scenarios = SCENARIO_MODE === 'throughput'
  ? {
      burst_transactions: {
        executor: 'constant-arrival-rate',
        rate: RATE,
        timeUnit: '1s',
        duration: DURATION,
        preAllocatedVUs: PRE_ALLOCATED_VUS,
        maxVUs: MAX_VUS,
      },
    }
  : {
      all_at_once_burst: {
        executor: 'per-vu-iterations',
        vus: BURST_VUS,
        iterations: 1,
        maxDuration: __ENV.MAX_DURATION || '2m',
      },
    };

export const options = {
  discardResponseBodies: true,
  scenarios,
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    checks: ['rate>0.98'],
  },
};

function pickCountryCode() {
  const countryCodes = [
    'AR', 'AU', 'BR', 'CA', 'CH', 'CL', 'CN', 'DE', 'ES', 'FR',
    'GB', 'IN', 'IT', 'JP', 'MX', 'NL', 'PT', 'SE', 'US', 'ZA',
  ];
  return countryCodes[Math.floor(Math.random() * countryCodes.length)];
}

function generatePayload() {
  const now = new Date(Date.now() - 5000);
  const pad = (n) => String(n).padStart(2, '0');
  const dateTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
  const suffix = `${__VU}-${__ITER}-${Date.now()}`;

  return {
    transactionId: `txn-${suffix}`,
    userId: `user-${Math.floor(Math.random() * 500000)}`,
    cardId: `card-${Math.floor(Math.random() * 1000000)}`,
    merchantId: `merchant-${Math.floor(Math.random() * 10000)}`,
    deviceId: `device-${Math.floor(Math.random() * 100000)}`,
    amount: Number((Math.random() * 4000 + 1).toFixed(2)),
    countryCode: pickCountryCode(),
    ipAddress: `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
    creationDateTime: dateTime,
  };
}

export default function () {
  const payload = JSON.stringify(generatePayload());
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: '10s',
  };

  const response = http.post(TARGET_URL, payload, params);

  const ok = check(response, {
    'status is 202': (r) => r.status === 202,
  });

  if (ok) {
    acceptedResponses.add(1);
  }
}
