import { Icon, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import type { ReactNode } from 'react';
import { useHaptic } from '@/shared/hooks/use-haptic';
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
  const haptic = useHaptic();

  const handleToggle = () => {
    haptic.impactOccurred('light');
    onToggle();
  };

  return (
    <motion.div {...pressScale} data-testid="role-card-root">
      <Tappable
        className="am-onboarding-role-card focusable"
        data-testid="role-card-trigger"
        data-selected={isSelected ? 'true' : 'false'}
        onClick={handleToggle}
      >
        <div className="am-onboarding-role-card__main">
          <div className="am-onboarding-role-card__icon">{icon}</div>
          <div className="am-onboarding-role-card__copy">
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
                className="am-onboarding-role-card__check"
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
              className="am-onboarding-role-card__details"
            >
              <div className="am-onboarding-role-card__divider" />
              <motion.div
                variants={previewContainer}
                initial="initial"
                animate="animate"
                className="am-onboarding-role-card__preview"
              >
                {children}
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </Tappable>
    </motion.div>
  );
}
