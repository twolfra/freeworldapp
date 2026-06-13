const BASE = '/api';

function getToken() {
  const user = JSON.parse(localStorage.getItem('currentUser') || 'null');
  return user?.token ?? null;
}

function handleUnauthorized() {
  localStorage.removeItem('currentUser');
  window.location.href = '/login';
}

async function upload(path, formData) {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    body: formData,
    headers: token ? { 'X-Session-Token': token } : {},
  });
  if (res.status === 401) { handleUnauthorized(); throw new Error('Session expired.'); }
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.error ?? `${res.status} ${res.statusText}`);
  }
  return res.json();
}

async function request(path, options = {}) {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'X-Session-Token': token } : {}),
      ...options.headers,
    },
    ...options,
  });
  if (res.status === 401) { handleUnauthorized(); throw new Error('Session expired.'); }
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
  login:               (body)  => request('/auth/login',  { method: 'POST', body: JSON.stringify(body) }),
  logout:              ()      => request('/auth/logout', { method: 'POST' }),
  verify:              (token) => request(`/auth/verify?token=${encodeURIComponent(token)}`),
  resendVerification:  (email) => request('/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email }) }),
};

export const users = {
  list: ()     => request('/users'),
  get:  (id)   => request(`/users/${id}`),
  create: (body) => request('/users', { method: 'POST', body: JSON.stringify(body) }),
};

export const offers = {
  list:       ()         => request('/offers'),
  listByUser: (userId)   => request(`/offers?offeredBy=${userId}`),
  get:        (id)       => request(`/offers/${id}`),
  create:     (body)     => request('/offers',    { method: 'POST',   body: JSON.stringify(body) }),
  update:     (id, body) => request(`/offers/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove:     (id)       => request(`/offers/${id}`, { method: 'DELETE' }),
};

export const requests = {
  list:       ()         => request('/requests'),
  listByUser: (userId)   => request(`/requests?requestedBy=${userId}`),
  get:        (id)       => request(`/requests/${id}`),
  create:     (body)     => request('/requests',    { method: 'POST',   body: JSON.stringify(body) }),
  update:     (id, body) => request(`/requests/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove:     (id)       => request(`/requests/${id}`, { method: 'DELETE' }),
};

export const images = {
  upload: (file) => {
    const data = new FormData();
    data.append('file', file);
    return upload('/images', data);
  },
};

export const subscriptions = {
  subscribe:   (body)                          => request('/subscriptions', { method: 'POST', body: JSON.stringify(body) }),
  unsubscribe: (subscriberId, subscribedToId)  =>
    request(`/subscriptions?subscriberId=${subscriberId}&subscribedToId=${subscribedToId}`, { method: 'DELETE' }),
  list:  (subscriberId)                        => request(`/subscriptions?subscriberId=${subscriberId}`),
  check: (subscriberId, subscribedToId)        =>
    request(`/subscriptions/check?subscriberId=${subscriberId}&subscribedToId=${subscribedToId}`),
  feed:  (subscriberId)                        => request(`/subscriptions/feed?subscriberId=${subscriberId}`),
};

export const messages = {
  send:             (body)             => request('/messages', { method: 'POST', body: JSON.stringify(body) }),
  getConversations: (userId)           => request(`/messages/conversations?userId=${userId}`),
  getConversation:  (userId, otherId)  => request(`/messages/conversation?userId=${userId}&otherId=${otherId}`),
  markRead:         (userId, otherId)  => request(`/messages/mark-read?userId=${userId}&otherId=${otherId}`, { method: 'POST' }),
  getUnreadCount:   (userId)           => request(`/messages/unread-count?userId=${userId}`),
};

export const likes = {
  like:    (targetType, targetId) => request(`/likes?targetType=${targetType}&targetId=${targetId}`, { method: 'POST' }),
  unlike:  (targetType, targetId) => request(`/likes?targetType=${targetType}&targetId=${targetId}`, { method: 'DELETE' }),
  check:   (targetType, targetId) => request(`/likes/check?targetType=${targetType}&targetId=${targetId}`),
  getUserLikes: (userId, targetType) =>
    request(`/likes?userId=${userId}${targetType ? `&targetType=${targetType}` : ''}`),
};
