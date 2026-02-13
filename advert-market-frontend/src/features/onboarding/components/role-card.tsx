import { Icon, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import type { ReactNode } from 'react';
import { pressScale } from '@/shared/ui';

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
    <motion.div {...pressScale}>
      <button
        type="button"
        className="focusable"
        onClick={onToggle}
        style={{
          display: 'flex',
          flexDirection: 'column',
          padding: '0',
          borderRadius: '12px',
          border: `1px solid ${isSelected ? 'var(--color-accent-primary)' : 'var(--color-border-separator)'}`,
          backgroundColor: isSelected
            ? 'rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.08)'
            : 'var(--color-background-base)',
          cursor: 'pointer',
          textAlign: 'left',
          width: '100%',
          transition: 'border-color 0.15s ease, background-color 0.15s ease',
          WebkitTapHighlightColor: 'transparent',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            padding: '16px',
            width: '100%',
          }}
        >
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '14px',
              backgroundColor: 'var(--color-background-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
              color: 'var(--color-accent-primary)',
            }}
          >
            {icon}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
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
                style={{ flexShrink: 0 }}
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
              style={{ overflow: 'hidden', width: '100%' }}
            >
              <div
                style={{
                  height: '1px',
                  backgroundColor: 'var(--color-border-separator)',
                  margin: '0 16px',
                }}
              />
              <motion.div
                variants={previewContainer}
                initial="initial"
                animate="animate"
                style={{ padding: '8px 16px 16px' }}
              >
                {children}
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </button>
    </motion.div>
  );
}
