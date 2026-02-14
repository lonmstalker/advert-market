import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Group, GroupItem, Text, Toggle } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { updateSettings } from '@/features/profile/api/profile-api';
import type { NotificationSettings } from '@/shared/api';
import { profileKeys } from '@/shared/api';
import { useToast } from '@/shared/hooks';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { BackButtonHandler } from '@/shared/ui';
import { fadeIn, staggerChildren } from '@/shared/ui/animations';

const DEBOUNCE_MS = 500;

export default function NotificationsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const notificationSettings = useSettingsStore((s) => s.notificationSettings);
  const setNotificationSetting = useSettingsStore((s) => s.setNotificationSetting);
  const setFromProfile = useSettingsStore((s) => s.setFromProfile);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const mutation = useMutation({
    mutationFn: (settings: NotificationSettings) => updateSettings({ notificationSettings: settings }),
    onSuccess: (updatedProfile) => {
      setFromProfile(updatedProfile);
      queryClient.setQueryData(profileKeys.me, updatedProfile);
    },
    onError: () => {
      showError(t('common.toast.saveFailed'));
    },
  });

  const scheduleSave = useCallback(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      const current = useSettingsStore.getState().notificationSettings;
      mutation.mutate(current);
    }, DEBOUNCE_MS);
  }, [mutation]);

  function handleToggle(group: keyof NotificationSettings, key: string, value: boolean) {
    setNotificationSetting(group, key, value);
    scheduleSave();
  }

  return (
    <motion.div {...fadeIn} style={{ padding: '16px' }}>
      <BackButtonHandler />
      <Text type="title1" weight="bold">
        {t('profile.notifications')}
      </Text>

      <motion.div {...staggerChildren} initial="initial" animate="animate" style={{ marginTop: 16 }}>
        <motion.div {...fadeIn}>
          <Group header={t('profile.notifications.deals')}>
            <GroupItem
              text={t('profile.notifications.newOffers')}
              after={
                <Toggle
                  isEnabled={notificationSettings.deals.newOffers}
                  onChange={(v) => handleToggle('deals', 'newOffers', v)}
                />
              }
            />
            <GroupItem
              text={t('profile.notifications.acceptReject')}
              after={
                <Toggle
                  isEnabled={notificationSettings.deals.acceptReject}
                  onChange={(v) => handleToggle('deals', 'acceptReject', v)}
                />
              }
            />
            <GroupItem
              text={t('profile.notifications.deliveryStatus')}
              after={
                <Toggle
                  isEnabled={notificationSettings.deals.deliveryStatus}
                  onChange={(v) => handleToggle('deals', 'deliveryStatus', v)}
                />
              }
            />
          </Group>
        </motion.div>

        <motion.div {...fadeIn}>
          <Group header={t('profile.notifications.financial')}>
            <GroupItem
              text={t('profile.notifications.deposits')}
              after={
                <Toggle
                  isEnabled={notificationSettings.financial.deposits}
                  onChange={(v) => handleToggle('financial', 'deposits', v)}
                />
              }
            />
            <GroupItem
              text={t('profile.notifications.payouts')}
              after={
                <Toggle
                  isEnabled={notificationSettings.financial.payouts}
                  onChange={(v) => handleToggle('financial', 'payouts', v)}
                />
              }
            />
            <GroupItem
              text={t('profile.notifications.escrow')}
              after={
                <Toggle
                  isEnabled={notificationSettings.financial.escrow}
                  onChange={(v) => handleToggle('financial', 'escrow', v)}
                />
              }
            />
          </Group>
        </motion.div>

        <motion.div {...fadeIn}>
          <Group header={t('profile.notifications.disputes')}>
            <GroupItem
              text={t('profile.notifications.opened')}
              after={
                <Toggle
                  isEnabled={notificationSettings.disputes.opened}
                  onChange={(v) => handleToggle('disputes', 'opened', v)}
                />
              }
            />
            <GroupItem
              text={t('profile.notifications.resolved')}
              after={
                <Toggle
                  isEnabled={notificationSettings.disputes.resolved}
                  onChange={(v) => handleToggle('disputes', 'resolved', v)}
                />
              }
            />
          </Group>
        </motion.div>
      </motion.div>
    </motion.div>
  );
}
