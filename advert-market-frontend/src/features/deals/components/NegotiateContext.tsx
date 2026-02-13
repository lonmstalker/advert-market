import { createContext, useContext } from 'react';

type NegotiateContextValue = {
  currentPriceNano: number;
  onSubmit: (priceNano: number, message?: string) => void;
  isPending: boolean;
};

const NegotiateContext = createContext<NegotiateContextValue | null>(null);

export function NegotiateProvider({
  children,
  currentPriceNano,
  onSubmit,
  isPending,
}: NegotiateContextValue & { children: React.ReactNode }) {
  return <NegotiateContext value={{ currentPriceNano, onSubmit, isPending }}>{children}</NegotiateContext>;
}

export function useNegotiateContext() {
  const ctx = useContext(NegotiateContext);
  if (!ctx) throw new Error('useNegotiateContext must be used within NegotiateProvider');
  return ctx;
}
