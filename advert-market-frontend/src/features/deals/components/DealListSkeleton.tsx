import { SkeletonElement } from '@telegram-tools/ui-kit';

function DealCardSkeleton() {
  return (
    <div className="flex flex-col gap-3 rounded-control border border-separator bg-bg-base px-4 py-[18px]">
      <div className="flex items-center gap-3">
        <SkeletonElement className="h-9 w-9 shrink-0 rounded-full" />
        <div className="flex-1">
          <SkeletonElement className="h-4 w-[120px] rounded-md" />
        </div>
        <SkeletonElement className="h-[18px] w-16 rounded-md" />
      </div>
      <div className="flex items-center justify-between">
        <SkeletonElement className="h-[22px] w-20 rounded-md" />
        <SkeletonElement className="h-3.5 w-[50px] rounded-md" />
      </div>
    </div>
  );
}

export function DealListSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="flex flex-col gap-4 px-4">
      {Array.from({ length: count }, (_, i) => (
        // biome-ignore lint/suspicious/noArrayIndexKey: static skeleton list, never reordered
        <DealCardSkeleton key={i} />
      ))}
    </div>
  );
}
