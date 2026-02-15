import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Group, GroupItem, Icon, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { updateSettings } from '@/features/profile/api/profile-api';
import { profileKeys } from '@/shared/api';
import { useToast } from '@/shared/hooks';
import { CURRENCIES } from '@/shared/lib/constants/currencies';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { BackButtonHandler } from '@/shared/ui';
import { pressScale, slideFromRight, staggerChildren } from '@/shared/ui/animations';

export default function CurrencyPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const displayCurrency = useSettingsStore((s) => s.displayCurrency);
  const setDisplayCurrency = useSettingsStore((s) => s.setDisplayCurrency);
  const setFromProfile = useSettingsStore((s) => s.setFromProfile);

  const mutation = useMutation({
    mutationFn: (currency: string) => updateSettings({ displayCurrency: currency }),
    onMutate: (currency) => {
      const prev = displayCurrency;
      setDisplayCurrency(currency);
      return { prev };
    },
    onSuccess: (updatedProfile) => {
      setFromProfile(updatedProfile);
      queryClient.setQueryData(profileKeys.me, updatedProfile);
    },
    onError: (_error, _currency, context) => {
      if (context?.prev) setDisplayCurrency(context.prev);
      showError(t('common.toast.saveFailed'));
    },
  });

  function handleSelect(code: string) {
    if (code === displayCurrency) return;
    mutation.mutate(code);
  }

  return (
    <motion.div {...slideFromRight} style={{ padding: '16px' }}>
      <BackButtonHandler />
      <Text type="title1" weight="bold">
        {t('profile.currency')}
      </Text>

      <motion.div {...staggerChildren} initial="initial" animate="animate" style={{ marginTop: 16 }}>
        <motion.div {...slideFromRight}>
          <Group header={t('profile.currency.title')} footer={t('profile.currency.hint')}>
            {CURRENCIES.map(({ code, symbol, labelKey }) => (
              <motion.div key={code} {...pressScale}>
                <GroupItem
                  text={t(labelKey)}
                  before={
                    <div
                      style={{
                        width: 36,
                        height: 36,
                        borderRadius: '50%',
                        background:
                          displayCurrency === code
                            ? 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)'
                            : 'var(--color-background-section)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'background 0.2s ease',
                      }}
                    >
                      <Text type="body" weight="bold" color={displayCurrency === code ? 'accent' : 'secondary'}>
                        {symbol}
                      </Text>
                    </div>
                  }
                  after={displayCurrency === code ? <Icon name="check" color="accent" /> : undefined}
                  onClick={() => handleSelect(code)}
                />
              </motion.div>
            ))}
          </Group>
        </motion.div>
      </motion.div>
    </motion.div>
  );
}
