import { SkeletonElement } from '@telegram-tools/ui-kit';

function TransactionItemSkeleton() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0' }}>
      <SkeletonElement style={{ width: 36, height: 36, borderRadius: '50%' }} />
      <div style={{ flex: 1 }}>
        <SkeletonElement style={{ width: 100, height: 14, borderRadius: 6 }} />
        <SkeletonElement style={{ width: 60, height: 12, borderRadius: 6, marginTop: 4 }} />
      </div>
      <SkeletonElement style={{ width: 64, height: 16, borderRadius: 6 }} />
    </div>
  );
}

export function TransactionListSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div style={{ padding: '0 16px' }}>
      <SkeletonElement style={{ width: 80, height: 12, borderRadius: 6, marginBottom: 8 }} />
      {Array.from({ length: count }, (_, i) => (
        // biome-ignore lint/suspicious/noArrayIndexKey: static skeleton list, never reordered
        <TransactionItemSkeleton key={i} />
      ))}
    </div>
  );
}
