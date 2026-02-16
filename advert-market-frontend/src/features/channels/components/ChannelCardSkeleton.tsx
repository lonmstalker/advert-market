import { SkeletonElement } from '@telegram-tools/ui-kit';

export function ChannelCardSkeleton() {
  return (
    <div className="am-catalog-skeleton-card">
      <div className="flex items-start gap-3">
        <SkeletonElement style={{ width: 44, height: 44, borderRadius: 999 }} />
        <div className="flex-1 flex flex-col gap-2">
          <SkeletonElement style={{ width: 180, height: 16, borderRadius: 6 }} />
          <SkeletonElement style={{ width: 120, height: 13, borderRadius: 6 }} />
        </div>
        <SkeletonElement style={{ width: 86, height: 20, borderRadius: 10 }} />
      </div>
      <div className="flex gap-2 mt-3.5">
        <SkeletonElement style={{ width: 70, height: 20, borderRadius: 999 }} />
        <SkeletonElement style={{ width: 84, height: 20, borderRadius: 999 }} />
      </div>
      <div className="grid grid-cols-3 gap-2 mt-3.5">
        <SkeletonElement style={{ width: '100%', height: 44, borderRadius: 10 }} />
        <SkeletonElement style={{ width: '100%', height: 44, borderRadius: 10 }} />
        <SkeletonElement style={{ width: '100%', height: 44, borderRadius: 10 }} />
      </div>
    </div>
  );
}
