const BASE = '/api';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    const msg = body?.error
      ?? body?.errors?.map((e) => e.defaultMessage).join(', ')
      ?? `${res.status} ${res.statusText}`;
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const auth = {
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
};

export const users = {
  list: () => request('/users'),
  get: (id) => request(`/users/${id}`),
  create: (body) => request('/users', { method: 'POST', body: JSON.stringify(body) }),
};

export const offers = {
  list: () => request('/offers'),
  get: (id) => request(`/offers/${id}`),
  create: (body) => request('/offers', { method: 'POST', body: JSON.stringify(body) }),
};
