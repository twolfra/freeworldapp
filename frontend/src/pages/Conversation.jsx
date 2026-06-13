import { useEffect, useRef, useState } from 'react';
import { messages as messagesApi, users } from '../api/client';
import styles from './Conversation.module.css';

export default function Conversation({ userId: otherId }) {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [otherUser, setOtherUser] = useState(null);
  const [msgs, setMsgs] = useState([]);
  const [content, setContent] = useState('');
  const [error, setError] = useState(null);
  const [sending, setSending] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    if (!currentUser) return;
    users.get(otherId).then(setOtherUser).catch(() => setError('User not found.'));
  }, [otherId]);

  useEffect(() => {
    if (!currentUser) return;

    // Force re-login for sessions created before token support was added
    if (!currentUser.token) {
      localStorage.removeItem('currentUser');
      window.location.href = '/login';
      return;
    }

    // Initial load + mark as read
    messagesApi.getConversation(currentUser.id, otherId)
      .then((msgs) => {
        setMsgs(msgs);
        messagesApi.markRead(currentUser.id, otherId).catch(() => {});
      })
      .catch(console.error);

    // SSE for live updates — replaces setInterval polling
    const es = new EventSource(
      `/api/messages/stream?userId=${currentUser.id}&token=${currentUser.token}`
    );

    es.addEventListener('message', (e) => {
      const msg = JSON.parse(e.data);
      if (msg.senderId === otherId || msg.recipientId === otherId) {
        setMsgs((prev) => {
          if (prev.some((m) => m.id === msg.id)) return prev;
          if (msg.recipientId === currentUser.id) {
            messagesApi.markRead(currentUser.id, otherId).catch(() => {});
          }
          return [...prev, msg];
        });
      }
    });

    // When the other person reads our messages, re-fetch to show read receipts
    es.addEventListener('read', (e) => {
      const { readerId } = JSON.parse(e.data);
      if (readerId === otherId) {
        messagesApi.getConversation(currentUser.id, otherId).then(setMsgs).catch(console.error);
      }
    });

    return () => es.close();
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!content.trim()) return;
    setSending(true);
    try {
      const newMsg = await messagesApi.send({ recipientId: otherId, content });
      setMsgs((prev) => [...prev, newMsg]);
      setContent('');
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  };

  // Index of the last sent message that has been read by the recipient
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
            placeholder="Write a message…"
            disabled={sending}
          />
          <button className={styles.sendBtn} type="submit" disabled={sending || !content.trim()}>
            Send
          </button>
        </form>
      </div>
    </main>
  );
}
