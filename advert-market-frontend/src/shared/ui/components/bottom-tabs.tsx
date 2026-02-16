import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { pressScale } from '@/shared/ui/animations';
import { DocumentIcon, PersonIcon, SearchIcon, WalletIcon } from '../icons';

const tabs = [
  { to: '/catalog', icon: SearchIcon, labelKey: 'tabs.catalog' },
  { to: '/deals', icon: DocumentIcon, labelKey: 'tabs.deals' },
  { to: '/wallet', icon: WalletIcon, labelKey: 'tabs.wallet' },
  { to: '/profile', icon: PersonIcon, labelKey: 'tabs.profile' },
] as const;

export function BottomTabs() {
  const { t } = useTranslation();
  const haptic = useHaptic();

  return (
    <nav aria-label="Main navigation" className="am-bottom-tabs">
      {tabs.map(({ to, icon: Icon, labelKey }) => (
        <NavLink
          key={to}
          to={to}
          onClick={() => haptic.selectionChanged()}
          className="am-tab-item"
          data-active={undefined}
        >
          {({ isActive }) => (
            <motion.span {...pressScale} className="am-tab-item" data-active={isActive}>
              <Icon width={24} height={24} />
              <Text type="caption2">{t(labelKey)}</Text>
            </motion.span>
          )}
        </NavLink>
      ))}
    </nav>
  );
}
