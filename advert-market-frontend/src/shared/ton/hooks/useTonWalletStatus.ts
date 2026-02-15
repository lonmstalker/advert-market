import { useIsConnectionRestored, useTonAddress, useTonConnectUI, useTonWallet } from '@tonconnect/ui-react';

export function useTonWalletStatus() {
  const [tonConnectUI] = useTonConnectUI();
  const wallet = useTonWallet();
  const friendlyAddress = useTonAddress();
  const isConnectionRestored = useIsConnectionRestored();

  return {
    isConnected: wallet !== null,
    isConnectionRestored,
    address: wallet?.account.address ?? null,
    friendlyAddress: wallet ? friendlyAddress : null,
    wallet,
    connect: async () => {
      tonConnectUI.openModal();
    },
    disconnect: async () => {
      await tonConnectUI.disconnect();
    },
  };
}
