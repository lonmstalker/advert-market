import { createContext, useContext } from 'react';
import type { CatalogFilters } from '../types/channel';

type ChannelFiltersContextValue = {
  currentFilters: CatalogFilters;
  onApply: (filters: CatalogFilters) => void;
  onReset: () => void;
};

const ChannelFiltersContext = createContext<ChannelFiltersContextValue | null>(null);

export function ChannelFiltersProvider({
  children,
  currentFilters,
  onApply,
  onReset,
}: ChannelFiltersContextValue & { children: React.ReactNode }) {
  return <ChannelFiltersContext value={{ currentFilters, onApply, onReset }}>{children}</ChannelFiltersContext>;
}

export function useChannelFiltersContext() {
  const ctx = useContext(ChannelFiltersContext);
  if (!ctx) throw new Error('useChannelFiltersContext must be used within ChannelFiltersProvider');
  return ctx;
}
