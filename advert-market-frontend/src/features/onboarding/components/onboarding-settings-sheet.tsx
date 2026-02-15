import { Group, GroupItem, Icon, Sheet, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { CURRENCIES } from '@/shared/lib/constants/currencies';
import { LANGUAGES } from '@/shared/lib/constants/languages';
import { useSettingsStore } from '@/shared/stores/settings-store';

type OnboardingSettingsSheetProps = {
  open: boolean;
  onClose: () => void;
};

function SettingsContent(_props: { onClose: () => void }) {
  const { t, i18n } = useTranslation();
  const displayCurrency = useSettingsStore((s) => s.displayCurrency);
  const setDisplayCurrency = useSettingsStore((s) => s.setDisplayCurrency);

  const currentLang = i18n.language;

  function handleLanguageSelect(code: string) {
    if (code === currentLang) return;
    i18n.changeLanguage(code);
  }

  function handleCurrencySelect(code: string) {
    if (code === displayCurrency) return;
    setDisplayCurrency(code);
  }

  return (
    <div style={{ padding: '0 0 16px' }}>
      <Group header={t('profile.language')}>
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
