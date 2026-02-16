import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Group, GroupItem, Icon, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { updateLanguage } from '@/features/profile/api/profile-api';
import { profileKeys } from '@/shared/api';
import { useAuth, useToast } from '@/shared/hooks';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { LANGUAGES } from '@/shared/lib/constants/languages';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { AppPageShell, BackButtonHandler } from '@/shared/ui';
import { pressScale, slideFromRight, staggerChildren } from '@/shared/ui/animations';

export default function LanguagePage() {
  const { t, i18n } = useTranslation();
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const haptic = useHaptic();
  const setFromProfile = useSettingsStore((s) => s.setFromProfile);

  const serverLang = profile?.languageCode ?? 'en';
  const [selected, setSelected] = useState(serverLang);

  const mutation = useMutation({
    mutationFn: updateLanguage,
    onSuccess: (updatedProfile) => {
      setFromProfile(updatedProfile);
      queryClient.setQueryData(profileKeys.me, updatedProfile);
    },
    onError: () => {
      setSelected(serverLang);
      i18n.changeLanguage(serverLang);
      showError(t('common.toast.saveFailed'));
    },
  });

  function handleSelect(langCode: string) {
    if (langCode === selected) return;
    haptic.selectionChanged();
    setSelected(langCode);
    i18n.changeLanguage(langCode);
    mutation.mutate(langCode);
  }

  return (
    <AppPageShell withTabsPadding={false} testId="profile-language-page">
      <BackButtonHandler />
      <Text type="title1" weight="bold">
        {t('profile.language')}
      </Text>

      <motion.div {...staggerChildren} initial="initial" animate="animate" className="mt-5">
        <motion.div {...slideFromRight}>
          <Group footer={t('profile.language.hint')}>
            {LANGUAGES.map(({ code, label, abbr }) => (
              <motion.div key={code} {...pressScale}>
                <GroupItem
                  text={label}
                  before={
                    <div
                      className={`am-icon-circle am-icon-circle--md transition-colors duration-200 text-[13px] font-bold tracking-[0.02em] ${
                        selected === code ? 'bg-soft-accent text-accent' : 'bg-bg-secondary text-fg-secondary'
                      }`}
                    >
                      {abbr}
                    </div>
                  }
                  after={selected === code ? <Icon name="check" color="accent" /> : undefined}
                  onClick={() => handleSelect(code)}
                />
              </motion.div>
            ))}
          </Group>
        </motion.div>
      </motion.div>
    </AppPageShell>
  );
}
