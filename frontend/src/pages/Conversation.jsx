import { useEffect, useRef, useState } from 'react';
import { messages as messagesApi, users } from '../api/client';
import styles from './Conversation.module.css';

export default function Conversation({ userId: otherId }) {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [otherUser, setOtherUser] = useState(null);
  const [msgs, setMsgs] = useState([]);
  const [content, setContent] = useState('');
  const [error, setError] = useState(null);
  const [wsReady, setWsReady] = useState(false);
  const wsRef = useRef(null);
  const bottomRef = useRef(null);

  useEffect(() => {
    if (!currentUser) return;
    users.get(otherId).then(setOtherUser).catch(() => setError('User not found.'));
  }, [otherId]);

  useEffect(() => {
    if (!currentUser?.token) return;

    // Initial history load
    messagesApi.getConversation(currentUser.id, otherId)
      .then((msgs) => {
        setMsgs(msgs);
        messagesApi.markRead(currentUser.id, otherId).catch(() => {});
      })
      .catch(console.error);

    // WebSocket — bidirectional: send via ws.send(), receive via ws.onmessage
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const ws = new WebSocket(
      `${proto}://${window.location.host}/ws/messages?userId=${currentUser.id}&token=${currentUser.token}`
    );
    wsRef.current = ws;

    ws.onopen  = () => setWsReady(true);
    ws.onclose = () => { setWsReady(false); wsRef.current = null; };
    ws.onerror = () => setError('Connection lost. Please refresh to reconnect.');

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);

      if (data.type === 'message') {
        if (data.senderId !== otherId && data.recipientId !== otherId) return;
        setMsgs((prev) => {
          if (prev.some((m) => m.id === data.id)) return prev;
          if (data.senderId === otherId) {
            messagesApi.markRead(currentUser.id, otherId).catch(() => {});
          }
          return [...prev, data];
        });
      } else if (data.type === 'read') {
        if (data.readerId === otherId) {
          messagesApi.getConversation(currentUser.id, otherId).then(setMsgs).catch(console.error);
        }
      }
    };

    return () => { ws.close(); wsRef.current = null; };
  }, [currentUser?.id, otherId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [msgs]);

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>Please <a href="/login">sign in</a> to send messages.</p>
    </main>
  );

  if (currentUser.id === otherId) return (
    <main className={styles.page}>
      <a href="/messages" className={styles.back}>← Back to Messages</a>
      <p className={styles.status}>You cannot message yourself.</p>
    </main>
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    const trimmed = content.trim();
    if (!trimmed || !wsRef.current || !wsReady) return;
    wsRef.current.send(JSON.stringify({ type: 'send', recipientId: otherId, content: trimmed }));
    setContent('');
  };

  const lastReadSentIndex = msgs.reduce(
    (acc, m, i) => (m.senderId === currentUser.id && m.readAt != null ? i : acc),
    -1
  );

  return (
    <main className={styles.page}>
      <a href="/messages" className={styles.back}>← Back to Messages</a>
      <div className={styles.thread}>
        <div className={styles.threadHeader}>
          @{otherUser?.username ?? '…'}
        </div>
        <div className={styles.messages}>
          {msgs.length === 0 && (
            <p className={styles.empty}>No messages yet. Say hello!</p>
          )}
          {msgs.map((m, i) => (
            <div
              key={m.id}
              className={`${styles.bubble} ${m.senderId === currentUser.id ? styles.sent : styles.received}`}
            >
              <p>{m.content}</p>
              <time>
                {new Date(m.createdAt).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
              </time>
              {i === lastReadSentIndex && (
                <span className={styles.seen}>Seen</span>
              )}
            </div>
          ))}
          <div ref={bottomRef} />
        </div>
        {error && <p className={styles.error}>{error}</p>}
        <form className={styles.form} onSubmit={handleSubmit}>
          <input
            className={styles.input}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={wsReady ? 'Write a message…' : 'Connecting…'}
            disabled={!wsReady}
          />
          <button className={styles.sendBtn} type="submit" disabled={!wsReady || !content.trim()}>
            Send
          </button>
        </form>
      </div>
    </main>
  );
}
