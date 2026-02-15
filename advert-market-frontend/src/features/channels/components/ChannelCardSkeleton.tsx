import { SkeletonElement } from '@telegram-tools/ui-kit';

export function ChannelCardSkeleton() {
  return (
    <div
      style={{
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
        borderRadius: 14,
        padding: 16,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <SkeletonElement style={{ width: 44, height: 44, borderRadius: '50%' }} />
        <div style={{ flex: 1 }}>
          <SkeletonElement style={{ width: 120, height: 16, borderRadius: 6 }} />
        </div>
        <SkeletonElement style={{ width: 60, height: 16, borderRadius: 6 }} />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
        <SkeletonElement style={{ width: 40, height: 14, borderRadius: 6 }} />
        <SkeletonElement style={{ width: 60, height: 20, borderRadius: 10 }} />
      </div>
    </div>
  );
}
