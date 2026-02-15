import { SkeletonElement } from '@telegram-tools/ui-kit';

export function WalletSkeleton() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Title */}
      <div style={{ padding: '16px 16px 0' }}>
        <SkeletonElement style={{ width: 100, height: 28, borderRadius: 6 }} />
      </div>

      {/* TON Connect card */}
      <div style={{ padding: '0 16px' }}>
        <div
          style={{
            padding: '12px',
            borderRadius: 14,
            background: 'var(--color-background-secondary)',
            border: '1px solid var(--color-border-separator)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 12,
          }}
        >
          <div style={{ flex: 1 }}>
            <SkeletonElement style={{ width: 140, height: 14, borderRadius: 6 }} />
            <SkeletonElement style={{ width: 180, height: 12, borderRadius: 6, marginTop: 4 }} />
          </div>
          <SkeletonElement style={{ width: 80, height: 32, borderRadius: 8 }} />
        </div>
      </div>

      {/* Hero balance */}
      <div style={{ padding: '8px 16px', textAlign: 'center' }}>
        <SkeletonElement style={{ width: 160, height: 40, borderRadius: 8, margin: '0 auto' }} />
      </div>

      {/* Stats row */}
      <div style={{ padding: '0 16px', display: 'flex', gap: 12 }}>
        <SkeletonElement style={{ flex: 1, height: 60, borderRadius: 12 }} />
        <SkeletonElement style={{ flex: 1, height: 60, borderRadius: 12 }} />
      </div>
    </div>
  );
}
