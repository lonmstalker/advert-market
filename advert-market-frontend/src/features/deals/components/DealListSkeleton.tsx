import { SkeletonElement } from '@telegram-tools/ui-kit';

function DealCardSkeleton() {
  return (
    <div
      style={{
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
        borderRadius: 14,
        padding: '14px 16px',
        display: 'flex',
        flexDirection: 'column',
        gap: 10,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <SkeletonElement style={{ width: 36, height: 36, borderRadius: '50%' }} />
        <div style={{ flex: 1 }}>
          <SkeletonElement style={{ width: 120, height: 16, borderRadius: 6 }} />
        </div>
        <SkeletonElement style={{ width: 64, height: 18, borderRadius: 6 }} />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <SkeletonElement style={{ width: 80, height: 22, borderRadius: 6 }} />
        <SkeletonElement style={{ width: 50, height: 14, borderRadius: 6 }} />
      </div>
    </div>
  );
}

export function DealListSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
      {Array.from({ length: count }, (_, i) => (
        // biome-ignore lint/suspicious/noArrayIndexKey: static skeleton list, never reordered
        <DealCardSkeleton key={i} />
      ))}
    </div>
  );
}
