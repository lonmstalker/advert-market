import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Group, GroupItem, Icon, Sheet, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { profileKeys } from '@/shared/api';
import { updateLanguage } from '@/shared/api/profile';
import { useToast } from '@/shared/hooks';
import { CURRENCIES, LANGUAGES } from '@/shared/lib/constants';
import { useSettingsStore } from '@/shared/stores/settings-store';

type OnboardingSettingsSheetProps = {
  open: boolean;
  onClose: () => void;
};

function SettingsContent() {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const displayCurrency = useSettingsStore((s) => s.displayCurrency);
  const setDisplayCurrency = useSettingsStore((s) => s.setDisplayCurrency);
  const setFromProfile = useSettingsStore((s) => s.setFromProfile);

  const currentLang = i18n.language;

  const languageMutation = useMutation({
    mutationFn: updateLanguage,
    onSuccess: (updatedProfile) => {
      setFromProfile(updatedProfile);
      queryClient.setQueryData(profileKeys.me, updatedProfile);
    },
  });

  function handleLanguageSelect(code: string) {
    if (code === currentLang) return;
    const prev = currentLang;
    i18n.changeLanguage(code);
    languageMutation.mutate(code, {
      onError: () => {
        i18n.changeLanguage(prev);
        showError(t('common.toast.saveFailed'));
      },
    });
  }

  function handleCurrencySelect(code: string) {
    if (code === displayCurrency) return;
    setDisplayCurrency(code);
  }

  return (
    <div style={{ padding: '0 0 16px' }}>
      <Group header={t('profile.language')} footer={t('profile.language.hint')}>
        {LANGUAGES.map(({ code, label, abbr }) => (
          <GroupItem
            key={code}
            text={label}
            before={
              <div
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: '50%',
                  background:
                    currentLang === code
                      ? 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)'
                      : 'var(--color-background-section)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 13,
                  fontWeight: 700,
                  color: currentLang === code ? 'var(--color-accent-primary)' : 'var(--color-foreground-secondary)',
                }}
              >
                {abbr}
              </div>
            }
            after={currentLang === code ? <Icon name="check" color="accent" /> : undefined}
            onClick={() => handleLanguageSelect(code)}
          />
        ))}
      </Group>

      <Group header={t('profile.currency')} footer={t('profile.currency.hint')}>
        {CURRENCIES.map(({ code, symbol, labelKey }) => (
          <GroupItem
            key={code}
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
                }}
              >
                <Text type="body" weight="bold" color={displayCurrency === code ? 'accent' : 'secondary'}>
                  {symbol}
                </Text>
              </div>
            }
            after={displayCurrency === code ? <Icon name="check" color="accent" /> : undefined}
            onClick={() => handleCurrencySelect(code)}
          />
        ))}
      </Group>
    </div>
  );
}

const sheetMap = {
  settings: SettingsContent,
};

export function OnboardingSettingsSheet({ open, onClose }: OnboardingSettingsSheetProps) {
  return <Sheet sheets={sheetMap} activeSheet="settings" opened={open} onClose={onClose} />;
}
