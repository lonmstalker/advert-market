import { createContext, useContext } from 'react';

type NegotiateContextValue = {
  actionLabelKey: string;
  reasonRequired: boolean;
  onSubmit: (reason?: string) => void;
  isPending: boolean;
};

const NegotiateContext = createContext<NegotiateContextValue | null>(null);

export function NegotiateProvider({
  children,
  actionLabelKey,
  reasonRequired,
  onSubmit,
  isPending,
}: NegotiateContextValue & { children: React.ReactNode }) {
  return (
    <NegotiateContext.Provider value={{ actionLabelKey, reasonRequired, onSubmit, isPending }}>
      {children}
    </NegotiateContext.Provider>
  );
}

export function useNegotiateContext() {
  const ctx = useContext(NegotiateContext);
  if (!ctx) throw new Error('useNegotiateContext must be used within NegotiateProvider');
  return ctx;
}
