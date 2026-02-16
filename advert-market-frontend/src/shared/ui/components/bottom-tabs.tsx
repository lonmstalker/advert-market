import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router';
import { useHaptic } from '@/shared/hooks/use-haptic';
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
          style={({ isActive }) => ({
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 3,
            flex: 1,
            height: '100%',
            borderRadius: 22,
            textDecoration: 'none',
            color: isActive ? 'var(--am-tabbar-active-color)' : 'var(--color-foreground-secondary)',
            background: isActive ? 'var(--am-tabbar-active-bg)' : 'transparent',
            border: isActive
              ? '1px solid color-mix(in srgb, var(--am-tabbar-active-color) 35%, transparent)'
              : '1px solid transparent',
            transition: 'color 0.16s ease, background 0.16s ease, transform 0.16s ease, border-color 0.16s ease',
            transform: isActive ? 'translateY(-1px)' : 'translateY(0)',
          })}
        >
          <Icon width={24} height={24} />
          <Text type="caption2">{t(labelKey)}</Text>
        </NavLink>
      ))}
    </nav>
  );
}
