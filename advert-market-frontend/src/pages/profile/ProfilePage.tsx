import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ComponentType, SVGProps } from 'react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useAuth } from '@/shared/hooks';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, staggerChildren } from '@/shared/ui/animations';
import { BellIcon, CoinIcon, GlobeIcon, PaletteIcon, SatelliteIcon } from '@/shared/ui/icons';
import { ProfileHero } from './components/ProfileHero';

const CURRENCY_LABELS: Record<string, string> = {
  USD: '$ USD',
  EUR: '\u20AC EUR',
  RUB: '\u20BD RUB',
};

const LANGUAGE_LABELS: Record<string, string> = {
  ru: '\u0420\u0443\u0441\u0441\u043A\u0438\u0439',
  en: 'English',
};

const SETTINGS_ITEMS: { key: string; icon: ComponentType<SVGProps<SVGSVGElement>>; route: string }[] = [
  { key: 'language', icon: GlobeIcon, route: '/profile/language' },
  { key: 'currency', icon: CoinIcon, route: '/profile/currency' },
  { key: 'notifications', icon: BellIcon, route: '/profile/notifications' },
];

function SettingsIcon({ icon: Icon }: { icon: ComponentType<SVGProps<SVGSVGElement>> }) {
  return (
    <div
      style={{
        width: 36,
        height: 36,
        borderRadius: '50%',
        background: 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Icon style={{ width: 18, height: 18, color: 'var(--color-accent-primary)' }} />
    </div>
  );
}

function formatMemberSince(dateStr: string, locale: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString(locale === 'ru' ? 'ru-RU' : 'en-US', {
    month: 'long',
    year: 'numeric',
  });
}

function getRoleBadge(interests: string[], t: (key: string) => string): string | null {
  if (interests.includes('advertiser') && interests.includes('channel_owner')) {
    return `${t('profile.role.advertiser')} & ${t('profile.role.channel_owner')}`;
  }
  if (interests.includes('advertiser')) return t('profile.role.advertiser');
  if (interests.includes('channel_owner')) return t('profile.role.channel_owner');
  return null;
}

export default function ProfilePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { profile } = useAuth();
  const displayCurrency = useSettingsStore((s) => s.displayCurrency);

  const displayName = profile?.displayName ?? '';
  const username = profile?.username ? `@${profile.username}` : '';
  const langCode = profile?.languageCode ?? 'en';

  const roleBadge = useMemo(
    () => (profile?.interests ? getRoleBadge(profile.interests, t) : null),
    [profile?.interests, t],
  );

  const memberSince = useMemo(() => {
    if (!profile?.createdAt) return null;
    const formatted = formatMemberSince(profile.createdAt, langCode);
    return t('profile.memberSince', { date: formatted });
  }, [profile?.createdAt, langCode, t]);

  return (
    <motion.div {...fadeIn} style={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      <ProfileHero
        displayName={displayName}
        username={username || undefined}
        roleBadge={roleBadge}
        memberSince={memberSince}
      />

      <motion.div
        {...staggerChildren}
        initial="initial"
        animate="animate"
        style={{ padding: '0 16px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}
      >
        <motion.div {...fadeIn}>
          <Group header={t('profile.channels')}>
            <EmptyState
              icon={<SatelliteIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
              title={t('profile.channels.empty.title')}
              description={t('profile.channels.empty.description')}
              actionLabel={t('profile.channels.empty.cta')}
              onAction={() => navigate('/profile/channels/new')}
            />
          </Group>
        </motion.div>

        <motion.div {...fadeIn}>
          <Group>
            <motion.div {...pressScale}>
              <GroupItem
                before={<SettingsIcon icon={PaletteIcon} />}
                text={t('creatives.title')}
                description={t('profile.creatives.description')}
                chevron
                onClick={() => navigate('/profile/creatives')}
              />
            </motion.div>
          </Group>
        </motion.div>

        <motion.div {...fadeIn}>
          <Group header={t('profile.settings')}>
            {SETTINGS_ITEMS.map(({ key, icon, route }) => (
              <motion.div key={key} {...pressScale}>
                <GroupItem
                  before={<SettingsIcon icon={icon} />}
                  text={t(`profile.${key}`)}
                  after={
                    key === 'language' ? (
                      <Text type="body" color="secondary">
                        {LANGUAGE_LABELS[langCode] ?? langCode}
                      </Text>
                    ) : key === 'currency' ? (
                      <Text type="body" color="secondary">
                        {CURRENCY_LABELS[displayCurrency] ?? displayCurrency}
                      </Text>
                    ) : undefined
                  }
                  chevron
                  onClick={() => navigate(route)}
                />
              </motion.div>
            ))}
          </Group>
        </motion.div>
      </motion.div>
    </motion.div>
  );
}
