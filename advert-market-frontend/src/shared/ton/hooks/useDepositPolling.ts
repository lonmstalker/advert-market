import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { dealKeys } from '@/shared/api/query-keys';
import { fetchDealDepositInfo } from '../api/deposit';
import type { DepositInfo } from '../types/ton';

export type DepositPollingOptions = {
  enabled: boolean;
  onConfirmed?: () => void;
  onTimeout?: () => void;
  timeoutMs?: number;
};

const DEFAULT_TIMEOUT_MS = 30 * 60 * 1000;

export function useDepositPolling(dealId: string, options: DepositPollingOptions) {
  const queryClient = useQueryClient();
  const [pollStartedAt, setPollStartedAt] = useState<number | null>(null);
  const [stopped, setStopped] = useState(false);

  useEffect(() => {
    if (!options.enabled) {
      setPollStartedAt(null);
      setStopped(false);
      return;
    }
    if (pollStartedAt === null) setPollStartedAt(Date.now());
  }, [options.enabled, pollStartedAt]);

  const pollIntervalMs = Number(import.meta.env.VITE_TON_DEPOSIT_POLL_INTERVAL_MS) || 10_000;
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;

  const query = useQuery({
    queryKey: dealKeys.deposit(dealId),
    queryFn: () => fetchDealDepositInfo(dealId),
    refetchOnWindowFocus: true,
    refetchInterval: options.enabled && !stopped ? pollIntervalMs : false,
    retry: 1,
  });

  const depositInfo: DepositInfo | null = query.data ?? null;

  const elapsed = useMemo(() => {
    if (!pollStartedAt) return 0;
    return Date.now() - pollStartedAt;
  }, [pollStartedAt]);

  useEffect(() => {
    if (!options.enabled || stopped) return;
    if (!pollStartedAt) return;
    if (elapsed <= timeoutMs) return;

    setStopped(true);
    options.onTimeout?.();
  }, [elapsed, options, pollStartedAt, stopped, timeoutMs]);

  useEffect(() => {
    if (!options.enabled || stopped) return;
    if (depositInfo?.status !== 'CONFIRMED') return;

    setStopped(true);

    // Ensure deal UI refreshes after the watcher flips status.
    queryClient.invalidateQueries({ queryKey: dealKeys.detail(dealId) });
    queryClient.invalidateQueries({ queryKey: dealKeys.lists() });
    options.onConfirmed?.();
  }, [dealId, depositInfo?.status, options, queryClient, stopped]);

  return {
    depositInfo,
    depositStatus: depositInfo?.status ?? null,
    confirmations: depositInfo?.currentConfirmations ?? null,
    requiredConfirmations: depositInfo?.requiredConfirmations ?? null,
    isPolling: options.enabled && !stopped,
    elapsed,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: () => query.refetch(),
  };
}
