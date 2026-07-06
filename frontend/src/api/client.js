import { getStoredUser, clearStoredUser } from '../auth/authStorage';

const BASE = '/api';

function getToken() {
  return getStoredUser()?.token ?? null;
}

function handleUnauthorized() {
  clearStoredUser();
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
    let msg = body?.error
      ?? body?.errors?.map((e) => e.defaultMessage).join(', ')
      ?? `${res.status} ${res.statusText}`;
    // correlation id (AP 4.5): lets an error toast be traced to the log line
    const requestId = res.headers.get('X-Request-Id');
    if (requestId && res.status >= 500) msg += ` (Ref: ${requestId})`;
    const err = new Error(msg);
    err.status = res.status;
    err.requestId = requestId;
    throw err;
  }
  if (res.status === 204) return null;
  return res.json();
}

// Build a query string from a params object, skipping empty values.
function buildQuery(params = {}) {
  const pairs = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
  return pairs.length ? `?${pairs.join('&')}` : '';
}

export const auth = {
  login:               (body)  => request('/auth/login',  { method: 'POST', body: JSON.stringify(body) }),
  logout:              ()      => request('/auth/logout', { method: 'POST' }),
  verify:              (token) => request(`/auth/verify?token=${encodeURIComponent(token)}`),
  resendVerification:  (email) => request('/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email }) }),
  forgotPassword:      (email) => request('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
  changePassword:      (oldPassword, newPassword) =>
    request('/auth/change-password', { method: 'POST', body: JSON.stringify({ oldPassword, newPassword }) }),
  resetPassword:       (token, newPassword) =>
    request('/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, newPassword }) }),
};

export const users = {
  list: ()     => request('/users'),
  get:  (id)   => request(`/users/${id}`),
  create: (body) => request('/users', { method: 'POST', body: JSON.stringify(body) }),
  update: (id, body) => request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  // Partial profile update — omitted/null fields are kept, "" clears a field.
  updateProfile: (id, fields) => request(`/users/${id}/profile`, { method: 'PUT', body: JSON.stringify(fields) }),
  remove: (id, password) =>
    request(`/users/${id}`, { method: 'DELETE', body: JSON.stringify({ password }) }),
  // DSGVO export: fetches with auth and triggers a browser download
  exportData: async () => {
    const token = getToken();
    const res = await fetch(`${BASE}/users/me/export`, {
      headers: token ? { 'X-Session-Token': token } : {},
    });
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'freeworld-export.json';
    a.click();
    URL.revokeObjectURL(url);
  },
};

export const offers = {
  list:       (includeCompleted = false) =>
    request(`/offers${includeCompleted ? '?includeCompleted=true' : ''}`),
  listByUser: (userId)   => request(`/offers?offeredBy=${userId}`),
  get:        (id)       => request(`/offers/${id}`),
  create:     (body)     => request('/offers',    { method: 'POST',   body: JSON.stringify(body) }),
  update:     (id, body) => request(`/offers/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove:     (id)       => request(`/offers/${id}`, { method: 'DELETE' }),
  setImages:  (id, images) =>
    request(`/offers/${id}/images`, { method: 'PUT', body: JSON.stringify({ images }) }),
  setStatus:  (id, status, reservedForId) =>
    request(`/offers/${id}/status`, { method: 'POST', body: JSON.stringify({ status, ...(reservedForId ? { reservedForId } : {}) }) }),
  interest:        (id) => request(`/offers/${id}/interest`, { method: 'POST' }),
  interestedCount: (id) => request(`/offers/${id}/interested`),
};

export const requests = {
  list:       (includeCompleted = false) =>
    request(`/requests${includeCompleted ? '?includeCompleted=true' : ''}`),
  listByUser: (userId)   => request(`/requests?requestedBy=${userId}`),
  get:        (id)       => request(`/requests/${id}`),
  create:     (body)     => request('/requests',    { method: 'POST',   body: JSON.stringify(body) }),
  update:     (id, body) => request(`/requests/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove:     (id)       => request(`/requests/${id}`, { method: 'DELETE' }),
  setImages:  (id, images) =>
    request(`/requests/${id}/images`, { method: 'PUT', body: JSON.stringify({ images }) }),
  setStatus:  (id, status) =>
    request(`/requests/${id}/status`, { method: 'POST', body: JSON.stringify({ status }) }),
};

// Local PLZ table lookup (no external geocoding calls).
export const geo = {
  postal: (q) => request(`/geo/postal?q=${encodeURIComponent(q)}`),
};

// Unified search endpoint (AP 3.1 + 3.5). Accepted params: type, q, category,
// lat, lon, radiusKm, sort (newest|nearest), withImage, includeCompleted, page, size.
export const search = {
  run: (params) => request(`/search${buildQuery(params)}`),
};

export const thanks = {
  create:  (offerId, text) => request(`/offers/${offerId}/thanks`, { method: 'POST', body: JSON.stringify({ text }) }),
  forUser: (userId)        => request(`/users/${userId}/thanks`),
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


export const contact = {
  send: (name, email, message) =>
    request('/contact', { method: 'POST', body: JSON.stringify({ name, email, message }) }),
};

export const notifications = {
  updatePreferences: (prefs) =>
    request('/notifications/preferences', { method: 'PUT', body: JSON.stringify(prefs) }),
  list:        () => request('/notifications'),
  markAllRead: () => request('/notifications/mark-all-read', { method: 'POST' }),
};

export const reports = {
  create: (targetType, targetId, reason, note) =>
    request('/reports', { method: 'POST', body: JSON.stringify({ targetType, targetId, reason, note }) }),
};

export const admin = {
  listUsers:     ()            => request('/admin/users'),
  block:         (userId)      => request(`/admin/users/${userId}/block`,   { method: 'POST' }),
  unblock:       (userId)      => request(`/admin/users/${userId}/unblock`, { method: 'POST' }),
  deleteUser:    (userId)      => request(`/admin/users/${userId}`, { method: 'DELETE' }),
  deleteOffer:   (id)          => request(`/admin/offers/${id}`,   { method: 'DELETE' }),
  deleteRequest: (id)          => request(`/admin/requests/${id}`, { method: 'DELETE' }),
  listReports:   (status)      => request(`/admin/reports${status ? `?status=${status}` : ''}`),
  resolveReport: (id)          => request(`/admin/reports/${id}/resolve`, { method: 'POST' }),
  dismissReport: (id)          => request(`/admin/reports/${id}/dismiss`, { method: 'POST' }),
  audit:         ()            => request('/admin/audit'),
  stats:         ()            => request('/admin/stats'),
};
