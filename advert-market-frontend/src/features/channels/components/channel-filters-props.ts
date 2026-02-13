import type { CatalogFilters } from '../types/channel';

type ChannelFiltersContentProps = {
  currentFilters: CatalogFilters;
  onApply: (filters: CatalogFilters) => void;
  onReset: () => void;
};

let filtersContentProps: ChannelFiltersContentProps | null = null;

export function setFiltersContentProps(props: ChannelFiltersContentProps) {
  filtersContentProps = props;
}

export function getFiltersContentProps() {
  return filtersContentProps;
}

export type { ChannelFiltersContentProps };
