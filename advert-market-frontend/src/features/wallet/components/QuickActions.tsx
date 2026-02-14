import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { pressScale } from '@/shared/ui/animations';

type QuickAction = {
  id: string;
  i18nKey: string;
  icon: React.ReactNode;
};

const ArrowDown = () => (
  <svg
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d="M12 5v14M5 12l7 7 7-7" />
  </svg>
);

const ArrowUp = () => (
  <svg
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d="M12 19V5M5 12l7-7 7 7" />
  </svg>
);

const ArrowLeftRight = () => (
  <svg
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d="M8 3l-4 4 4 4M16 21l4-4-4-4M4 7h16M20 17H4" />
  </svg>
);

const ACTIONS: QuickAction[] = [
  { id: 'topUp', i18nKey: 'wallet.quickAction.topUp', icon: <ArrowDown /> },
  { id: 'withdraw', i18nKey: 'wallet.quickAction.withdraw', icon: <ArrowUp /> },
  {
    id: 'transfer',
    i18nKey: 'wallet.quickAction.transfer',
    icon: <ArrowLeftRight />,
  },
];

export function QuickActions() {
  const { t } = useTranslation();
  const haptic = useHaptic();

  const handleClick = () => {
    haptic.impactOccurred('light');
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', gap: 24 }}>
      {ACTIONS.map((action) => (
        <motion.div {...pressScale} key={action.id} style={{ textAlign: 'center' }}>
          <button
            type="button"
            style={{
              width: 56,
              height: 56,
              borderRadius: '50%',
              backgroundColor: 'var(--color-accent-primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              color: '#fff',
              border: 'none',
              padding: 0,
            }}
            aria-label={t(action.i18nKey)}
            onClick={handleClick}
          >
            {action.icon}
          </button>
          <div style={{ marginTop: 8 }}>
            <Text type="caption1">{t(action.i18nKey)}</Text>
          </div>
        </motion.div>
      ))}
    </div>
  );
}
