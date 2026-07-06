import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { admin as adminApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t } from '../i18n';
import { ConfirmModal, useToast } from '../components/ui';
import styles from './Admin.module.css';

export default function Admin() {
  const { user: currentUser } = useAuth();
  const [tab, setTab] = useState('reports');

  if (!currentUser) {
    return (
      <main className={styles.page}>
        <p className={styles.status}>
          {t('admin.signIn')} <Link to="/login">{t('admin.signInLink')}</Link>
        </p>
      </main>
    );
  }
  if (currentUser.role !== 'ADMIN') {
    return (
      <main className={styles.page}>
        <p className={styles.status}>{t('admin.denied')}</p>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t('admin.title')}</h1>
      <div className={styles.tabs}>
        <button
          className={tab === 'reports' ? styles.tabActive : styles.tab}
          onClick={() => setTab('reports')}
        >
          {t('admin.tabReports')}
        </button>
        <button
          className={tab === 'users' ? styles.tabActive : styles.tab}
          onClick={() => setTab('users')}
        >
          {t('admin.tabUsers')}
        </button>
        <button
          className={tab === 'audit' ? styles.tabActive : styles.tab}
          onClick={() => setTab('audit')}
        >
          {t('admin.tabAudit')}
        </button>
      </div>
      {tab === 'reports' && <ReportsTab currentUser={currentUser} />}
      {tab === 'users' && <UsersTab currentUser={currentUser} />}
      {tab === 'audit' && <AuditTab />}
    </main>
  );
}

function ReportsTab({ currentUser }) {
  const [reports, setReports] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(null);
  const [pending, setPending] = useState(null); // { message, run }
  const toast = useToast();

  function load() {
    adminApi.listReports('OPEN')
      .then(setReports)
      .catch((e) => setError(e.message));
  }
  useEffect(load, []);

  async function act(fn, id) {
    setBusy(id);
    try {
      await fn();
      load();
    } catch (e) {
      toast.error(e.message);
    } finally {
      setBusy(null);
    }
  }

  function postLink(r) {
    if (r.targetType === 'OFFER') return `/offers/${r.targetId}`;
    if (r.targetType === 'REQUEST') return `/requests/${r.targetId}`;
    return `/users/${r.targetId}`;
  }

  function deletePost(r) {
    const del = r.targetType === 'OFFER' ? adminApi.deleteOffer : adminApi.deleteRequest;
    setPending({
      message: t('admin.confirmDelete'),
      run: () => act(async () => {
        await del(r.targetId);
        await adminApi.resolveReport(r.id);
      }, r.id),
    });
  }

  function blockAuthor(r) {
    setPending({
      message: t('admin.confirmBlock'),
      run: () => act(async () => {
        await adminApi.block(r.targetAuthorId);
        await adminApi.resolveReport(r.id);
      }, r.id),
    });
  }

  if (error) return <p className={styles.status}>{error}</p>;
  if (!reports) return <p className={styles.status}>{t('admin.loading')}</p>;
  if (reports.length === 0) return <p className={styles.status}>{t('admin.noReports')}</p>;

  return (
    <div className={styles.list}>
      {reports.map((r) => (
        <div key={r.id} className={styles.reportCard}>
          <div className={styles.reportHead}>
            <span className={styles.reasonPill}>{t('report.reason.' + r.reason) || r.reason}</span>
            <span className={styles.targetType}>{t('admin.target.' + r.targetType) || r.targetType}</span>
            <span className={styles.dim}>{new Date(r.createdAt).toLocaleString()}</span>
          </div>
          <div className={styles.reportBody}>
            <p>
              <strong>{t('admin.reportedItem')}:</strong>{' '}
              {r.targetExists
                ? <Link to={postLink(r)}>{r.targetTitle || r.targetId}</Link>
                : <em>{t('admin.targetGone')}</em>}
            </p>
            {r.targetAuthorUsername && (
              <p>
                <strong>{t('admin.author')}:</strong>{' '}
                <Link to={`/users/${r.targetAuthorId}`}>{r.targetAuthorUsername}</Link>
                {r.targetAuthorBlocked && <span className={styles.blockedTag}>{t('admin.blocked')}</span>}
              </p>
            )}
            <p><strong>{t('admin.reportedBy')}:</strong> {r.reporterUsername}</p>
            {r.note && <p className={styles.note}>“{r.note}”</p>}
          </div>
          <div className={styles.reportActions}>
            {r.targetExists && r.targetType !== 'USER' && (
              <button className={styles.dangerBtn} disabled={busy === r.id} onClick={() => deletePost(r)}>
                {t('admin.deletePost')}
              </button>
            )}
            {r.targetAuthorId && r.targetAuthorId !== currentUser.id && !r.targetAuthorBlocked && (
              <button className={styles.warnBtn} disabled={busy === r.id} onClick={() => blockAuthor(r)}>
                {t('admin.blockAuthor')}
              </button>
            )}
            <button
              className={styles.ghostBtn}
              disabled={busy === r.id}
              onClick={() => act(() => adminApi.dismissReport(r.id), r.id)}
            >
              {t('admin.dismiss')}
            </button>
          </div>
        </div>
      ))}
      <ConfirmModal
        open={pending !== null}
        message={pending?.message}
        danger
        onConfirm={async () => {
          try {
            await pending.run();
          } finally {
            setPending(null);
          }
        }}
        onCancel={() => setPending(null)}
      />
    </div>
  );
}

function UsersTab({ currentUser }) {
  const [users, setUsers] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(null);
  const [pending, setPending] = useState(null); // { message, run }
  const toast = useToast();

  function load() {
    adminApi.listUsers()
      .then(setUsers)
      .catch((e) => setError(e.message));
  }
  useEffect(load, []);

  async function act(fn, id) {
    setBusy(id);
    try {
      await fn();
      load();
    } catch (e) {
      toast.error(e.message);
    } finally {
      setBusy(null);
    }
  }

  function toggleBlock(u) {
    setPending({
      message: u.blocked ? t('admin.confirmUnblock') : t('admin.confirmBlock'),
      run: () => act(() => (u.blocked ? adminApi.unblock(u.id) : adminApi.block(u.id)), u.id),
    });
  }

  function deleteUser(u) {
    setPending({
      message: t('admin.confirmDeleteUser'),
      run: () => act(() => adminApi.deleteUser(u.id), u.id),
    });
  }

  if (error) return <p className={styles.status}>{error}</p>;
  if (!users) return <p className={styles.status}>{t('admin.loading')}</p>;

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>{t('admin.colUser')}</th>
            <th>{t('admin.colEmail')}</th>
            <th>{t('admin.colRole')}</th>
            <th>{t('admin.colPosts')}</th>
            <th>{t('admin.colStatus')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id} className={u.blocked ? styles.blockedRow : undefined}>
              <td><Link to={`/users/${u.id}`}>{u.username}</Link></td>
              <td className={styles.dim}>{u.email}</td>
              <td>{u.role === 'ADMIN' ? <span className={styles.adminTag}>ADMIN</span> : 'USER'}</td>
              <td className={styles.dim}>{u.offerCount + u.requestCount}</td>
              <td>{u.blocked ? <span className={styles.blockedTag}>{t('admin.blocked')}</span> : t('admin.active')}</td>
              <td>
                {u.id === currentUser.id ? (
                  <span className={styles.dim}>{t('admin.you')}</span>
                ) : (
                  <>
                    <button
                      className={u.blocked ? styles.warnBtn : styles.dangerBtn}
                      disabled={busy === u.id}
                      onClick={() => toggleBlock(u)}
                    >
                      {u.blocked ? t('admin.unblock') : t('admin.block')}
                    </button>
                    <button
                      className={styles.dangerBtn}
                      disabled={busy === u.id}
                      onClick={() => deleteUser(u)}
                      style={{ marginLeft: '0.4rem' }}
                    >
                      {t('admin.deleteUser')}
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <ConfirmModal
        open={pending !== null}
        message={pending?.message}
        danger
        onConfirm={async () => {
          try {
            await pending.run();
          } finally {
            setPending(null);
          }
        }}
        onCancel={() => setPending(null)}
      />
    </div>
  );
}

function AuditTab() {
  const [entries, setEntries] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    adminApi.audit()
      .then(setEntries)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className={styles.status}>{error}</p>;
  if (!entries) return <p className={styles.status}>{t('admin.loading')}</p>;
  if (entries.length === 0) return <p className={styles.status}>{t('admin.noAudit')}</p>;

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>{t('admin.colWhen')}</th>
            <th>{t('admin.colAdmin')}</th>
            <th>{t('admin.colAction')}</th>
            <th>{t('admin.colTarget')}</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((e) => (
            <tr key={e.id}>
              <td className={styles.dim}>{new Date(e.createdAt).toLocaleString()}</td>
              <td>{e.adminUsername}</td>
              <td>{t('admin.audit.' + e.action) || e.action}</td>
              <td className={styles.dim}>
                {(t('admin.target.' + e.targetType) || e.targetType)} · {e.targetId.slice(0, 8)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
