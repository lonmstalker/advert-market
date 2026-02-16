import { SkeletonElement } from '@telegram-tools/ui-kit';

export function WalletSkeleton() {
  return (
    <div className="am-finance-page">
      <div className="am-finance-stack">
        {/* BalanceCard skeleton */}
        <div
          style={{
            borderRadius: 18,
            background: 'var(--am-card-surface)',
            border: '1px solid var(--am-card-border)',
            overflow: 'hidden',
            padding: '14px 14px 18px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <SkeletonElement style={{ width: 80, height: 14, borderRadius: 6 }} />
            <SkeletonElement style={{ width: 80, height: 32, borderRadius: 8 }} />
          </div>
          <div style={{ textAlign: 'center', marginTop: 16 }}>
            <SkeletonElement style={{ width: 140, height: 32, borderRadius: 8, margin: '0 auto' }} />
            <SkeletonElement style={{ width: 80, height: 16, borderRadius: 6, margin: '6px auto 0' }} />
          </div>
        </div>
        {/* MetricRow skeleton */}
        <div
          style={{
            display: 'flex',
            borderRadius: 18,
            background: 'var(--am-card-surface)',
            border: '1px solid var(--am-card-border)',
            overflow: 'hidden',
          }}
        >
          <div style={{ flex: 1, padding: '14px 12px', textAlign: 'center' }}>
            <SkeletonElement style={{ width: 60, height: 20, borderRadius: 6, margin: '0 auto' }} />
            <SkeletonElement style={{ width: 70, height: 12, borderRadius: 6, margin: '4px auto 0' }} />
          </div>
          <div style={{ width: 1, alignSelf: 'stretch', background: 'var(--color-border-separator)' }} />
          <div style={{ flex: 1, padding: '14px 12px', textAlign: 'center' }}>
            <SkeletonElement style={{ width: 30, height: 20, borderRadius: 6, margin: '0 auto' }} />
            <SkeletonElement style={{ width: 90, height: 12, borderRadius: 6, margin: '4px auto 0' }} />
          </div>
        </div>
        {/* Transaction rows skeleton */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <SkeletonElement style={{ width: 140, height: 20, borderRadius: 6 }} />
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '12px',
                borderRadius: 18,
                background: 'var(--am-card-surface)',
                border: '1px solid var(--am-card-border)',
              }}
            >
              <SkeletonElement style={{ width: 40, height: 40, borderRadius: '50%', flexShrink: 0 }} />
              <div style={{ flex: 1 }}>
                <SkeletonElement style={{ width: 100, height: 16, borderRadius: 6 }} />
                <SkeletonElement style={{ width: 140, height: 12, borderRadius: 6, marginTop: 4 }} />
              </div>
              <div style={{ textAlign: 'right' }}>
                <SkeletonElement style={{ width: 60, height: 16, borderRadius: 6, marginLeft: 'auto' }} />
                <SkeletonElement style={{ width: 40, height: 12, borderRadius: 6, marginTop: 4, marginLeft: 'auto' }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
