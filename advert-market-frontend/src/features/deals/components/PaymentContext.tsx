import { createContext, useContext } from 'react';

type PaymentContextValue = {
  dealId: string;
  onClose: () => void;
};

const PaymentContext = createContext<PaymentContextValue | null>(null);

export function PaymentProvider({ children, dealId, onClose }: PaymentContextValue & { children: React.ReactNode }) {
  return <PaymentContext.Provider value={{ dealId, onClose }}>{children}</PaymentContext.Provider>;
}

export function usePaymentContext() {
  const ctx = useContext(PaymentContext);
  if (!ctx) throw new Error('usePaymentContext must be used within PaymentProvider');
  return ctx;
}
