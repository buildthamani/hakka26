import { useCallback, useEffect, useState } from 'react';

/**
 * Counts down from `seconds` to 0 in real time, once per second.
 * `restart` begins a fresh countdown.
 */
export function useCountdown(seconds: number) {
  const [remaining, setRemaining] = useState(seconds);
  // Bumping this key re-runs the interval effect, which restarts the timer.
  const [runId, setRunId] = useState(0);

  useEffect(() => {
    setRemaining(seconds);
    const id = setInterval(() => {
      setRemaining((value) => {
        if (value <= 1) {
          clearInterval(id);
          return 0;
        }
        return value - 1;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [seconds, runId]);

  const restart = useCallback(() => setRunId((value) => value + 1), []);

  return { remaining, isRunning: remaining > 0, restart };
}
