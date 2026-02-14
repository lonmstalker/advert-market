import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useAuth } from '@/shared/hooks';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { ChannelAvatar, EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, staggerChildren } from '@/shared/ui/animations';

const CURRENCY_LABELS: Record<string, string> = {
  USD: '$ USD',
  EUR: '\u20AC EUR',
  RUB: '\u20BD RUB',
};

const LANGUAGE_LABELS: Record<string, string> = {
  ru: '\u0420\u0443\u0441\u0441\u043A\u0438\u0439',
  en: 'English',
};

const SETTINGS_ITEMS = [
  { key: 'language', emoji: '\uD83C\uDF10', route: '/profile/language' },
  { key: 'currency', emoji: '\uD83D\uDCB0', route: '/profile/currency' },
  { key: 'notifications', emoji: '\uD83D\uDD14', route: '/profile/notifications' },
] as const;

function SettingsIcon({ emoji, active = false }: { emoji: string; active?: boolean }) {
  return (
    <div
      style={{
        width: 36,
        height: 36,
        borderRadius: '50%',
        background: active
          ? 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)'
          : 'var(--color-background-section)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <span style={{ fontSize: 18 }}>{emoji}</span>
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

  const accountDescription = [username, roleBadge, memberSince].filter(Boolean).join(' \u00B7 ');

  return (
    <motion.div {...fadeIn} style={{ padding: '16px' }}>
      <Text type="title1" weight="bold">
        {t('profile.title')}
      </Text>

      <motion.div {...staggerChildren} initial="initial" animate="animate" style={{ marginTop: 16 }}>
        <motion.div {...fadeIn}>
          <Group header={t('profile.account')}>
            <GroupItem
              before={<ChannelAvatar title={displayName || 'U'} size="lg" />}
              text={displayName}
              description={accountDescription || undefined}
            />
          </Group>
        </motion.div>

        <motion.div {...fadeIn}>
          <EmptyState
            emoji={'\uD83D\uDCE1'}
            title={t('profile.channels.empty.title')}
            description={t('profile.channels.empty.description')}
            actionLabel={t('profile.channels.empty.cta')}
            onAction={() => navigate('/profile/channels/new')}
          />
        </motion.div>

        <motion.div {...fadeIn}>
          <Group header={t('creatives.title')}>
            <motion.div {...pressScale}>
              <GroupItem
                before={<SettingsIcon emoji={'\uD83C\uDFA8'} />}
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
            {SETTINGS_ITEMS.map(({ key, emoji, route }) => (
              <motion.div key={key} {...pressScale}>
                <GroupItem
                  before={<SettingsIcon emoji={emoji} />}
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
