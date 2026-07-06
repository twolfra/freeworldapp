import { useEffect, useState } from 'react';
import { messages as messagesApi } from '../api/client';

// Shared unread-message counter used by Navbar (desktop badge) and TabBar
// (mobile badge). Fetches the initial count over HTTP, then keeps a
// WebSocket open and re-fetches on every `message` / `read` event.
// Each component that calls the hook owns its own socket — the backend
// fan-out (`ChatWebSocketHandler`) supports multiple connections per user.
export default function useUnreadCount(currentUser) {
  const [unread, setUnread] = useState(0);
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

    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const ws = new WebSocket(
      `${proto}://${window.location.host}/ws/messages`
    );
    // First frame must authenticate — the token no longer travels in the URL.
    ws.onopen = () => ws.send(JSON.stringify({ type: 'auth', token }));
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'message' || data.type === 'read') refreshCount();
    };

    return () => ws.close();
  }, [userId, token]);

  return currentUser ? unread : 0;
}
