import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Group, GroupItem, Icon, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { updateLanguage } from '@/features/profile/api/profile-api';
import { profileKeys } from '@/shared/api';
import { useAuth, useToast } from '@/shared/hooks';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { BackButtonHandler } from '@/shared/ui';
import { pressScale, slideFromRight, staggerChildren } from '@/shared/ui/animations';

const LANGUAGES = [
  { code: 'ru', label: '\u0420\u0443\u0441\u0441\u043A\u0438\u0439', abbr: 'RU' },
  { code: 'en', label: 'English', abbr: 'EN' },
] as const;

export default function LanguagePage() {
  const { t, i18n } = useTranslation();
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const { showError } = useToast();
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
    setSelected(langCode);
    i18n.changeLanguage(langCode);
    mutation.mutate(langCode);
  }

  return (
    <motion.div {...slideFromRight} style={{ padding: '16px' }}>
      <BackButtonHandler />
      <Text type="title1" weight="bold">
        {t('profile.language')}
      </Text>

      <motion.div {...staggerChildren} initial="initial" animate="animate" style={{ marginTop: 16 }}>
        <motion.div {...slideFromRight}>
          <Group footer={t('profile.language.hint')}>
            {LANGUAGES.map(({ code, label, abbr }) => (
              <motion.div key={code} {...pressScale}>
                <GroupItem
                  text={label}
                  before={
                    <div
                      style={{
                        width: 36,
                        height: 36,
                        borderRadius: '50%',
                        background:
                          selected === code
                            ? 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)'
                            : 'var(--color-background-section)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'background 0.2s ease',
                        fontSize: 13,
                        fontWeight: 700,
                        color: selected === code ? 'var(--color-accent-primary)' : 'var(--color-foreground-secondary)',
                        letterSpacing: '0.02em',
                      }}
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
    </motion.div>
  );
}
