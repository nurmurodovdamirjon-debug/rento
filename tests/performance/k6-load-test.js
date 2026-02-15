import { check, group, sleep } from 'k6';
import http from 'k6/http';
import { Rate, Trend } from 'k6/metrics';

// ----- Custom metrics -----
const errorRate = new Rate('errors');
const listingDuration = new Trend('listing_duration', true);
const searchDuration = new Trend('search_duration', true);
const authDuration = new Trend('auth_duration', true);

// ----- Config -----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';
const AUTH_URL = __ENV.AUTH_URL || 'http://localhost:3001';
const API_URL = __ENV.API_URL || 'http://localhost:3002';
const CHAT_URL = __ENV.CHAT_URL || 'http://localhost:3003';

// ----- Scenarios -----
export const options = {
  scenarios: {
    // Smoke test — minimal load
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      tags: { test_type: 'smoke' },
      exec: 'browseListings',
    },
    // Average load — typical daily usage
    average_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },
        { duration: '3m', target: 20 },
        { duration: '1m', target: 0 },
      ],
      tags: { test_type: 'average' },
      exec: 'userJourney',
      startTime: '40s',
    },
    // Stress test — high load
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '3m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 0 },
      ],
      tags: { test_type: 'stress' },
      exec: 'browseListings',
      startTime: '6m',
    },
    // Spike test — sudden spike
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 150 },
        { duration: '30s', target: 150 },
        { duration: '10s', target: 0 },
      ],
      tags: { test_type: 'spike' },
      exec: 'browseListings',
      startTime: '16m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed: ['rate<0.05'],
    errors: ['rate<0.1'],
    listing_duration: ['p(95)<1500'],
    search_duration: ['p(95)<2000'],
    auth_duration: ['p(95)<1000'],
  },
};

// ----- Helper -----
const headers = { 'Content-Type': 'application/json' };

function jsonHeaders(token) {
  const h = { 'Content-Type': 'application/json' };
  if (token) h['Authorization'] = `Bearer ${token}`;
  return h;
}

// ----- Scenarios -----

/**
 * Browse listings — unauthenticated, most common path
 */
export function browseListings() {
  group('Browse Listings', () => {
    // Health check
    const health = http.get(`${API_URL}/health`);
    check(health, { 'health ok': (r) => r.status === 200 });

    // Get listings page 1
    const res1 = http.get(`${API_URL}/api/listings?city=tashkent&page=1&perPage=20`);
    listingDuration.add(res1.timings.duration);
    const ok1 = check(res1, {
      'listings 200': (r) => r.status === 200,
      'has items': (r) => {
        try { return r.json().data.items.length >= 0; } catch { return false; }
      },
    });
    errorRate.add(!ok1);

    sleep(1);

    // Get listings page 2
    const res2 = http.get(`${API_URL}/api/listings?city=tashkent&page=2&perPage=20`);
    listingDuration.add(res2.timings.duration);
    errorRate.add(res2.status !== 200);

    sleep(0.5);

    // Search
    const searchRes = http.get(`${API_URL}/api/listings/search?query=kvartira&city=tashkent`);
    searchDuration.add(searchRes.timings.duration);
    check(searchRes, { 'search 200': (r) => r.status === 200 });
    errorRate.add(searchRes.status !== 200);

    sleep(1);
  });
}

/**
 * Full user journey — auth, browse, search, view listing
 */
export function userJourney() {
  let token = null;

  group('Auth Flow', () => {
    // Send OTP
    const otpRes = http.post(`${AUTH_URL}/api/auth/send-otp`,
      JSON.stringify({ phone: `+99890${Math.floor(1000000 + Math.random() * 9000000)}` }),
      { headers }
    );
    authDuration.add(otpRes.timings.duration);
    check(otpRes, { 'otp sent': (r) => r.status === 200 || r.status === 201 || r.status === 429 });

    sleep(0.5);

    // In test env with fixed OTP
    const verifyRes = http.post(`${AUTH_URL}/api/auth/verify-otp`,
      JSON.stringify({ phone: '+998901234567', code: '123456' }),
      { headers }
    );
    authDuration.add(verifyRes.timings.duration);
    if (verifyRes.status === 200) {
      try {
        const data = verifyRes.json().data;
        if (data && data.accessToken) token = data.accessToken;
      } catch {}
    }
    errorRate.add(verifyRes.status !== 200);
  });

  sleep(0.5);

  group('Authenticated Browsing', () => {
    // Get my profile
    const profileRes = http.get(`${API_URL}/api/users/me`, {
      headers: jsonHeaders(token),
    });
    check(profileRes, { 'profile ok': (r) => r.status === 200 || r.status === 401 });

    sleep(0.5);

    // Browse listings
    const listRes = http.get(`${API_URL}/api/listings?city=tashkent&page=1&perPage=20`, {
      headers: jsonHeaders(token),
    });
    listingDuration.add(listRes.timings.duration);
    check(listRes, { 'listings ok': (r) => r.status === 200 });

    sleep(0.5);

    // Search
    const searchRes = http.get(`${API_URL}/api/listings/search?query=kvartira`, {
      headers: jsonHeaders(token),
    });
    searchDuration.add(searchRes.timings.duration);
    check(searchRes, { 'search ok': (r) => r.status === 200 });

    sleep(0.5);

    // Nearby listings
    const nearbyRes = http.get(`${API_URL}/api/listings/nearby?latitude=41.2867&longitude=69.2072&radiusKm=3`, {
      headers: jsonHeaders(token),
    });
    listingDuration.add(nearbyRes.timings.duration);
    check(nearbyRes, { 'nearby ok': (r) => r.status === 200 });

    sleep(0.3);

    // Get favorites
    const favRes = http.get(`${API_URL}/api/favorites`, {
      headers: jsonHeaders(token),
    });
    check(favRes, { 'favorites ok': (r) => r.status === 200 || r.status === 401 });

    sleep(0.3);

    // Notifications
    const notifRes = http.get(`${API_URL}/api/notifications/unread-count`, {
      headers: jsonHeaders(token),
    });
    check(notifRes, { 'notif ok': (r) => r.status === 200 || r.status === 401 });
  });

  sleep(1);
}

/**
 * Default — maps to browseListings for simple runs
 */
export default function () {
  browseListings();
}
