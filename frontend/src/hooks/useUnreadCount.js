import { useEffect, useState } from 'react';
import { messages as messagesApi, notifications as notificationsApi } from '../api/client';

// Shared unread-message counter used by Navbar (desktop badge) and TabBar
// (mobile badge). Fetches the initial count over HTTP, then keeps a
// WebSocket open and re-fetches on every `message` / `read` event.
// Each component that calls the hook owns its own socket — the backend
// fan-out (`ChatWebSocketHandler`) supports multiple connections per user.
// options.withNotifications: also track the in-app notification unread count
// (fetched from /api/notifications, incremented live on `notification` frames).
export default function useUnreadCount(currentUser, options = {}) {
  const { withNotifications = false } = options;
  const [unread, setUnread] = useState(0);
  const [notifUnread, setNotifUnread] = useState(0);
  const userId = currentUser?.id;
  const token = currentUser?.token;

  useEffect(() => {
    if (!userId || !token) return;

    const refreshCount = () => {
      messagesApi.getUnreadCount(userId)
        .then((r) => setUnread(r.count))
        .catch(() => {});
    };

    refreshCount();

    const refreshNotifications = () => {
      notificationsApi.list()
        .then((r) => setNotifUnread(r.unread))
        .catch(() => {});
    };
    if (withNotifications) refreshNotifications();

    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const ws = new WebSocket(
      `${proto}://${window.location.host}/ws/messages`
    );
    // First frame must authenticate — the token no longer travels in the URL.
    ws.onopen = () => ws.send(JSON.stringify({ type: 'auth', token }));
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'message' || data.type === 'read') refreshCount();
      if (withNotifications && data.type === 'notification') refreshNotifications();
    };

    const onMarkedRead = () => { if (withNotifications) refreshNotifications(); };
    window.addEventListener('fw:notifications-read', onMarkedRead);

    return () => {
      window.removeEventListener('fw:notifications-read', onMarkedRead);
      ws.close();
    };
  }, [userId, token, withNotifications]);

  if (!currentUser) return { unread: 0, notifUnread: 0 };
  return { unread, notifUnread };
}
