import { useTonConnectUI, useTonWallet } from '@tonconnect/ui-react';
import { useCallback, useState } from 'react';
import { mapTonConnectError, type TonTransactionError } from '../lib/ton-errors';

export type TonTransactionParams = {
  address: string;
  amountNano: string;
  validUntil?: number;
};

export function useTonTransaction() {
  const [tonConnectUI] = useTonConnectUI();
  const wallet = useTonWallet();

  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<TonTransactionError | null>(null);

  const send = useCallback(
    async (params: TonTransactionParams) => {
      if (isPending) return;

      setIsPending(true);
      setError(null);

      try {
        if (!wallet) {
          const err: TonTransactionError = { code: 'WALLET_NOT_CONNECTED' };
          setError(err);
          throw err;
        }

        const transaction = {
          validUntil: params.validUntil ?? Math.floor(Date.now() / 1000) + 600,
          messages: [
            {
              address: params.address,
              amount: params.amountNano,
            },
          ],
        };

        // `sendTransaction` type is loosely defined by the library; keep it runtime-safe.
        await tonConnectUI.sendTransaction(transaction);
      } catch (err: unknown) {
        const mapped =
          typeof err === 'object' && err !== null && 'code' in err
            ? (err as TonTransactionError)
            : mapTonConnectError(err);
        setError(mapped);
        throw mapped;
      } finally {
        setIsPending(false);
      }
    },
    [isPending, tonConnectUI, wallet],
  );

  return { send, isPending, error };
}
