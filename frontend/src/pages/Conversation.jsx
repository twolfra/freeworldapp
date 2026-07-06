import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { messages as messagesApi, offers as offersApi, requests as requestsApi, users } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t } from '../i18n';
import styles from './Conversation.module.css';

export default function Conversation() {
  const { userId: otherId } = useParams();
  const { user: currentUser } = useAuth();
  const [otherUser, setOtherUser] = useState(null);
  const [msgs, setMsgs] = useState([]);
  const [content, setContent] = useState('');
  const [error, setError] = useState(null);
  const [wsReady, setWsReady] = useState(false);
  const wsRef = useRef(null);
  const bottomRef = useRef(null);

  useEffect(() => {
    if (!currentUser) return;
    users.get(otherId).then(setOtherUser).catch(() => setError(t('conv.userNotFound')));
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
      `${proto}://${window.location.host}/ws/messages`
    );
    wsRef.current = ws;

    // First frame must authenticate; the socket is "ready" once the server confirms.
    ws.onopen  = () => ws.send(JSON.stringify({ type: 'auth', token: currentUser.token }));
    ws.onclose = () => { setWsReady(false); wsRef.current = null; };
    ws.onerror = () => setError(t('conv.connLost'));

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);

      if (data.type === 'auth_ok') {
        setWsReady(true);
      } else if (data.type === 'message') {
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

  // Context card — the most recent message in the thread that carries an
  // offer/request context ("I'm interested" messages).
  const lastCtxMsg = [...msgs].reverse().find((m) => m.contextType && m.contextId);
  const ctxType = lastCtxMsg?.contextType ?? null;
  const ctxId = lastCtxMsg?.contextId ?? null;
  const [ctxFetched, setCtxFetched] = useState(null); // { id, post }

  useEffect(() => {
    if (!ctxType || !ctxId) return;
    let cancelled = false;
    const api = ctxType === 'OFFER' ? offersApi : requestsApi;
    api.get(ctxId)
      .then((post) => { if (!cancelled) setCtxFetched({ id: ctxId, post }); })
      .catch(() => {}); // deleted post → no card
    return () => { cancelled = true; };
  }, [ctxType, ctxId]);

  // Only show the card when the fetched post matches the current context.
  const ctxPost = ctxFetched && ctxFetched.id === ctxId ? ctxFetched.post : null;

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>Please <Link to="/login">{t('conv.signInLink')}</Link> {t('conv.signIn').replace('Please {link} ', '')}</p>
    </main>
  );

  if (currentUser.id === otherId) return (
    <main className={styles.page}>
      <Link to="/messages" className={styles.back}>{t('conv.back')}</Link>
      <p className={styles.status}>{t('conv.cannotSelf')}</p>
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

  const otherDeleted = msgs.some((m) =>
    (m.senderId === otherId && m.senderDeleted) || (m.recipientId === otherId && m.recipientDeleted));

  return (
    <main className={styles.page}>
      <Link to="/messages" className={styles.back}>{t('conv.back')}</Link>
      <div className={styles.thread}>
        <div className={styles.threadHeader}>
          {otherDeleted ? t('user.deleted') : (otherUser?.username ?? '…')}
        </div>
        {ctxPost && (
          <Link
            to={`${ctxType === 'OFFER' ? '/offers' : '/requests'}/${ctxId}`}
            className={styles.contextCard}
          >
            {ctxPost.imageUrl && <img src={ctxPost.imageUrl} alt="" />}
            <div>
              <span className={styles.contextType}>
                {ctxType === 'OFFER' ? t('conv.ctxOffer') : t('conv.ctxRequest')}
              </span>
              <span className={styles.contextTitle}>{ctxPost.title}</span>
            </div>
          </Link>
        )}
        <div className={styles.messages}>
          {msgs.length === 0 && (
            <p className={styles.empty}>{t('conv.noMessages')}</p>
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
                <span className={styles.seen}>{t('conv.seen')}</span>
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
            placeholder={wsReady ? t('conv.placeholder') : t('conv.connecting')}
            disabled={!wsReady}
          />
          <button className={styles.sendBtn} type="submit" disabled={!wsReady || !content.trim()}>
            {t('conv.send')}
          </button>
        </form>
      </div>
    </main>
  );
}
