import { Icon, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import type { ReactNode } from 'react';
import { pressScale, Tappable } from '@/shared/ui';

type RoleCardProps = {
  icon: ReactNode;
  title: string;
  hint: string;
  isSelected: boolean;
  onToggle: () => void;
  children?: ReactNode;
};

const previewContainer = {
  initial: {},
  animate: { transition: { staggerChildren: 0.06 } },
};

export function RoleCard({ icon, title, hint, isSelected, onToggle, children }: RoleCardProps) {
  return (
    <motion.div {...pressScale} data-testid="role-card-root">
      <Tappable
        className={`focusable flex flex-col p-0 rounded-[12px] cursor-pointer text-left w-full overflow-hidden transition-[border-color,background-color] duration-150 ease-in-out [-webkit-tap-highlight-color:transparent] border ${isSelected ? 'border-accent bg-soft-accent' : 'border-separator bg-bg-base'}`}
        data-testid="role-card-trigger"
        onClick={onToggle}
      >
        <div className="flex items-center gap-4 p-4 w-full">
          <div className="size-14 rounded-[14px] bg-bg-secondary flex items-center justify-center shrink-0 text-accent">
            {icon}
          </div>
          <div className="flex-1 min-w-0">
            <Text type="body" weight="medium">
              {title}
            </Text>
            <Text type="caption1" color="secondary">
              {hint}
            </Text>
          </div>
          <AnimatePresence>
            {isSelected && (
              <motion.div
                initial={{ opacity: 0, scale: 0.5 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.5 }}
                transition={{ duration: 0.15 }}
                className="shrink-0"
              >
                <Icon name="check" color="accent" size="24px" />
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <AnimatePresence>
          {isSelected && children && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.25, ease: 'easeOut' }}
              className="overflow-hidden w-full"
            >
              <div className="h-px bg-separator mx-4" />
              <motion.div variants={previewContainer} initial="initial" animate="animate" className="px-4 pt-2 pb-4">
                {children}
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </Tappable>
    </motion.div>
  );
}
