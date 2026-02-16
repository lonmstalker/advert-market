import { SkeletonElement } from '@telegram-tools/ui-kit';

export function ChannelCardSkeleton() {
  return (
    <div className="bg-bg-base border border-separator rounded-row p-4">
      <div className="flex items-center gap-3">
        <SkeletonElement style={{ width: 44, height: 44, borderRadius: '50%' }} />
        <div className="flex-1">
          <SkeletonElement style={{ width: 120, height: 16, borderRadius: 6 }} />
        </div>
        <SkeletonElement style={{ width: 60, height: 16, borderRadius: 6 }} />
      </div>
      <div className="flex justify-between mt-3">
        <SkeletonElement style={{ width: 40, height: 14, borderRadius: 6 }} />
        <SkeletonElement style={{ width: 60, height: 20, borderRadius: 10 }} />
      </div>
    </div>
  );
}
