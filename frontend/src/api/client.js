const BASE = '/api';

async function upload(path, formData) {
  const res = await fetch(`${BASE}${path}`, { method: 'POST', body: formData });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.error ?? `${res.status} ${res.statusText}`);
  }
  return res.json();
}

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
  listByUser: (userId) => request(`/offers?offeredBy=${userId}`),
  get: (id) => request(`/offers/${id}`),
  create: (body) => request('/offers', { method: 'POST', body: JSON.stringify(body) }),
};

export const requests = {
  list: () => request('/requests'),
  listByUser: (userId) => request(`/requests?requestedBy=${userId}`),
  get: (id) => request(`/requests/${id}`),
  create: (body) => request('/requests', { method: 'POST', body: JSON.stringify(body) }),
};

export const images = {
  upload: (file) => {
    const data = new FormData();
    data.append('file', file);
    return upload('/images', data);
  },
};

export const subscriptions = {
  subscribe: (body) => request('/subscriptions', { method: 'POST', body: JSON.stringify(body) }),
  unsubscribe: (subscriberId, subscribedToId) =>
    request(`/subscriptions?subscriberId=${subscriberId}&subscribedToId=${subscribedToId}`, { method: 'DELETE' }),
  list: (subscriberId) => request(`/subscriptions?subscriberId=${subscriberId}`),
  check: (subscriberId, subscribedToId) =>
    request(`/subscriptions/check?subscriberId=${subscriberId}&subscribedToId=${subscribedToId}`),
  feed: (subscriberId) => request(`/subscriptions/feed?subscriberId=${subscriberId}`),
};

export const messages = {
  send: (body) => request('/messages', { method: 'POST', body: JSON.stringify(body) }),
  getConversations: (userId) => request(`/messages/conversations?userId=${userId}`),
  getConversation: (userId, otherId) => request(`/messages/conversation?userId=${userId}&otherId=${otherId}`),
};
