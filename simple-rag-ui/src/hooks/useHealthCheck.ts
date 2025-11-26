import { useCallback, useEffect, useRef, useState } from 'react';
import { apiUrl, API_BASE_URL } from '../config/api';

type Options = {
  url?: string;
  steadyIntervalMs?: number;
  retryIntervalsMs?: number[];
};

// health URL resolution uses central config (apiUrl)

const DEFAULT_RETRY_INTERVALS = [5000, 5000, 10000, 30000, 60000, 300000];

export default function useHealthCheck({
  url,
  steadyIntervalMs = 60000,
  retryIntervalsMs,
}: Options = {}) {
  const resolvedUrl = url ?? apiUrl('/api/health/ping');
  const retryIntervals = retryIntervalsMs ?? DEFAULT_RETRY_INTERVALS;
  const [up, setUp] = useState<boolean>(true);
  const [lastChecked, setLastChecked] = useState<number | null>(null);
  const failureCount = useRef<number>(0);
  const timer = useRef<number | null>(null);

  const clearTimer = () => {
    if (timer.current !== null) {
      window.clearTimeout(timer.current);
      timer.current = null;
    }
  };

  const jitter = (ms: number) => {
    const factor = 0.75 + Math.random() * 0.5; // 0.75..1.25
    return Math.max(200, Math.round(ms * factor));
  };

  const scheduleNext = (ms: number) => {
    clearTimer();
    timer.current = window.setTimeout(() => void check(), ms);
  };

  const handleFailure = () => {
    failureCount.current += 1;
    setUp(false);
    const idx = Math.min(failureCount.current - 1, retryIntervals.length - 1);
    scheduleNext(jitter(retryIntervals[idx]));
  };

  const handleSuccess = () => {
    failureCount.current = 0;
    if (!up) setUp(true);
    setLastChecked(Date.now());
    scheduleNext(steadyIntervalMs);
  };

  const check = useCallback(async () => {
    // debug: log when a check starts
    // eslint-disable-next-line no-console
    console.debug('[useHealthCheck] checking', { url: resolvedUrl });
    try {
      if (typeof navigator !== 'undefined' && !navigator.onLine) {
        // eslint-disable-next-line no-console
        console.debug('[useHealthCheck] navigator.offline');
        handleFailure();
        return;
      }
      const res = await fetch(resolvedUrl, { cache: 'no-store', method: 'GET' });
      if (res.ok) {
        // eslint-disable-next-line no-console
        console.debug('[useHealthCheck] success', { status: res.status });
        handleSuccess();
      } else {
        // eslint-disable-next-line no-console
        console.debug('[useHealthCheck] non-ok response', { status: res.status, url: resolvedUrl });
        handleFailure();
      }
    } catch (err) {
      // eslint-disable-next-line no-console
      console.debug('[useHealthCheck] fetch error', err, { url: resolvedUrl });
      handleFailure();
    }
  }, [resolvedUrl, steadyIntervalMs, retryIntervals]);

  useEffect(() => {
    clearTimer();
    failureCount.current = 0;
    void check();
    return () => clearTimer();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [check]);

  useEffect(() => {
    const onVisible = () => void check();
    window.addEventListener('visibilitychange', onVisible);
    window.addEventListener('focus', onVisible);
    return () => {
      window.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('focus', onVisible);
    };
  }, [check]);

  const forceCheck = useCallback(() => void check(), [check]);

  return { up, lastChecked, forceCheck };
}
