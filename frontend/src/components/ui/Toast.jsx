import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { t } from '../../i18n';
import styles from './Toast.module.css';

const ToastContext = createContext(null);
const AUTO_DISMISS_MS = 4000;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((list) => list.filter((item) => item.id !== id));
  }, []);

  const push = useCallback(
    (type, message) => {
      nextId.current += 1;
      const id = nextId.current;
      setToasts((list) => [...list, { id, type, message }]);
      setTimeout(() => dismiss(id), AUTO_DISMISS_MS);
      return id;
    },
    [dismiss],
  );

  const toast = useMemo(
    () => ({
      success: (message) => push('success', message),
      error: (message) => push('error', message),
      info: (message) => push('info', message),
      dismiss,
    }),
    [push, dismiss],
  );

  return (
    <ToastContext.Provider value={toast}>
      {children}
      {createPortal(
        <div className={styles.stack} aria-live="polite">
          {toasts.map((item) => (
            <div key={item.id} className={[styles.toast, styles[item.type]].join(' ')}>
              <span className={styles.message}>{item.message}</span>
              <button
                type="button"
                className={styles.dismiss}
                onClick={() => dismiss(item.id)}
                aria-label={t('ui.dismiss')}
              >
                ×
              </button>
            </div>
          ))}
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components -- provider + hook belong together; toast state is ephemeral so a fast-refresh remount is harmless
export function useToast() {
  return useContext(ToastContext);
}
