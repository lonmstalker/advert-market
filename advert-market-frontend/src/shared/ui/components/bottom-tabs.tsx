import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router';
import { DocumentIcon, PersonIcon, SearchIcon, WalletIcon } from '../icons';

const tabs = [
  { to: '/catalog', icon: SearchIcon, labelKey: 'tabs.catalog' },
  { to: '/deals', icon: DocumentIcon, labelKey: 'tabs.deals' },
  { to: '/wallet', icon: WalletIcon, labelKey: 'tabs.wallet' },
  { to: '/profile', icon: PersonIcon, labelKey: 'tabs.profile' },
] as const;

export function BottomTabs() {
  const { t } = useTranslation();

  return (
    <nav
      style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        display: 'flex',
        justifyContent: 'space-around',
        alignItems: 'center',
        height: '56px',
        backgroundColor: 'var(--color-background-base)',
        borderTop: '0.5px solid var(--color-border-separator)',
        paddingBottom: 'env(safe-area-inset-bottom)',
        zIndex: 100,
      }}
    >
      {tabs.map(({ to, icon: Icon, labelKey }) => (
        <NavLink
          key={to}
          to={to}
          style={({ isActive }) => ({
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '4px',
            flex: 1,
            height: '100%',
            textDecoration: 'none',
            color: isActive ? 'var(--color-accent-primary)' : 'var(--color-foreground-secondary)',
            transition: 'color 0.15s ease',
          })}
        >
          <Icon width={24} height={24} />
          <Text type="caption2" style={{ color: 'inherit' }}>
            {t(labelKey)}
          </Text>
        </NavLink>
      ))}
    </nav>
  );
}
