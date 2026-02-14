import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
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

export default function ProfilePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { profile } = useAuth();
  const displayCurrency = useSettingsStore((s) => s.displayCurrency);

  const displayName = profile?.displayName ?? '';
  const username = profile?.username ? `@${profile.username}` : '';
  const langCode = profile?.languageCode ?? 'en';

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
              description={username}
            />
          </Group>
        </motion.div>

        <motion.div {...fadeIn}>
          <EmptyState
            emoji="📡"
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
            <motion.div {...pressScale}>
              <GroupItem
                text={t('profile.language')}
                after={
                  <Text type="body" color="secondary">
                    {LANGUAGE_LABELS[langCode] ?? langCode}
                  </Text>
                }
                chevron
                onClick={() => navigate('/profile/language')}
              />
            </motion.div>
            <motion.div {...pressScale}>
              <GroupItem
                text={t('profile.currency')}
                after={
                  <Text type="body" color="secondary">
                    {CURRENCY_LABELS[displayCurrency] ?? displayCurrency}
                  </Text>
                }
                chevron
                onClick={() => navigate('/profile/currency')}
              />
            </motion.div>
            <motion.div {...pressScale}>
              <GroupItem text={t('profile.notifications')} chevron onClick={() => navigate('/profile/notifications')} />
            </motion.div>
          </Group>
        </motion.div>
      </motion.div>
    </motion.div>
  );
}
