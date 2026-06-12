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

  const fetchMessages = () => {
    if (!currentUser) return;
    messagesApi.getConversation(currentUser.id, otherId)
      .then(setMsgs)
      .catch(console.error);
  };

  useEffect(() => {
    fetchMessages();
    const interval = setInterval(fetchMessages, 5000);
    return () => clearInterval(interval);
  }, [currentUser?.id, otherId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [msgs]);

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>Please <a href="/login">sign in</a> to send messages.</p>
    </main>
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!content.trim()) return;
    setSending(true);
    try {
      const newMsg = await messagesApi.send({ senderId: currentUser.id, recipientId: otherId, content });
      setMsgs((prev) => [...prev, newMsg]);
      setContent('');
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  };

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
          {msgs.map((m) => (
            <div
              key={m.id}
              className={`${styles.bubble} ${m.senderId === currentUser.id ? styles.sent : styles.received}`}
            >
              <p>{m.content}</p>
              <time>
                {new Date(m.createdAt).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
              </time>
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
