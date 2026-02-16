import { SkeletonElement } from '@telegram-tools/ui-kit';
import { AppPageShell } from '@/shared/ui';

export function WalletSkeleton() {
  return (
    <AppPageShell variant="finance" testId="wallet-page-shell">
      <div className="flex flex-col gap-5">
        {/* BalanceCard skeleton */}
        <div className="rounded-card bg-card-surface border border-card-border overflow-hidden px-5 pt-5 pb-7">
          <div className="flex justify-between items-center">
            <SkeletonElement style={{ width: 80, height: 14, borderRadius: 6 }} />
            <SkeletonElement style={{ width: 80, height: 32, borderRadius: 8 }} />
          </div>
          <div className="text-center mt-6">
            <SkeletonElement style={{ width: 160, height: 40, borderRadius: 8, margin: '0 auto' }} />
            <SkeletonElement style={{ width: 80, height: 16, borderRadius: 6, margin: '8px auto 0' }} />
            <SkeletonElement style={{ width: 100, height: 24, borderRadius: 999, margin: '14px auto 0' }} />
          </div>
        </div>
        {/* Quick actions skeleton */}
        <div className="grid grid-cols-2 gap-3">
          {[0, 1].map((i) => (
            <div
              key={i}
              className="rounded-[18px] bg-card-surface border border-card-border min-h-[84px] flex items-center justify-center"
            >
              <div className="flex flex-col items-center gap-2.5">
                <SkeletonElement style={{ width: 40, height: 40, borderRadius: '50%' }} />
                <SkeletonElement style={{ width: 60, height: 12, borderRadius: 6 }} />
              </div>
            </div>
          ))}
        </div>
        {/* MetricRow skeleton */}
        <div className="flex rounded-card bg-card-surface border border-card-border overflow-hidden">
          <div className="flex-1 py-[18px] px-4 text-center">
            <SkeletonElement style={{ width: 60, height: 20, borderRadius: 6, margin: '0 auto' }} />
            <SkeletonElement style={{ width: 70, height: 12, borderRadius: 6, margin: '6px auto 0' }} />
          </div>
          <div className="w-px self-stretch bg-separator" />
          <div className="flex-1 py-[18px] px-4 text-center">
            <SkeletonElement style={{ width: 30, height: 20, borderRadius: 6, margin: '0 auto' }} />
            <SkeletonElement style={{ width: 90, height: 12, borderRadius: 6, margin: '6px auto 0' }} />
          </div>
        </div>
        {/* Transaction rows skeleton */}
        <div className="flex flex-col gap-4">
          <SkeletonElement style={{ width: 160, height: 20, borderRadius: 6 }} />
          <div className="flex flex-col gap-2.5">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className="flex items-center gap-3.5 p-4 rounded-[16px] bg-card-surface border border-card-border"
              >
                <SkeletonElement style={{ width: 40, height: 40, borderRadius: '50%', flexShrink: 0 }} />
                <div className="flex-1">
                  <SkeletonElement style={{ width: 100, height: 16, borderRadius: 6 }} />
                  <SkeletonElement style={{ width: 140, height: 12, borderRadius: 6, marginTop: 6 }} />
                </div>
                <div className="text-right">
                  <SkeletonElement style={{ width: 60, height: 16, borderRadius: 6, marginLeft: 'auto' }} />
                  <SkeletonElement
                    style={{ width: 40, height: 12, borderRadius: 6, marginTop: 6, marginLeft: 'auto' }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </AppPageShell>
  );
}
