import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Group, GroupItem, Icon, Text } from '@telegram-tools/ui-kit';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { CurrencyMode } from '@/shared/api/auth';
import { updateLanguage, updateSettings } from '@/shared/api/profile';
import { profileKeys } from '@/shared/api/query-keys';
import { useToast } from '@/shared/hooks';
import { CURRENCIES, LANGUAGES } from '@/shared/lib/constants';
import { trackOnboardingEvent } from '@/shared/lib/onboarding-analytics';
import { useSettingsStore } from '@/shared/stores/settings-store';

type LocaleCurrencyEditorMode = 'onboarding' | 'profile';
type LocaleCurrencyView = 'main' | 'language' | 'currency';

type LocaleCurrencyEditorProps = {
  mode: LocaleCurrencyEditorMode;
  onContinue?: () => void;
};

type UndoState = {
  previousLanguage: string;
  previousCurrency: string;
  nextCurrency: string;
};

function modeToAnalytics(mode: CurrencyMode): 'auto' | 'manual' {
  return mode === 'AUTO' ? 'auto' : 'manual';
}

function normalizeLanguage(code: string): string {
  return (code.split('-')[0] ?? code).toLowerCase();
}

export function LocaleCurrencyEditor({ mode, onContinue }: LocaleCurrencyEditorProps) {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const [view, setView] = useState<LocaleCurrencyView>('main');
  const [undoState, setUndoState] = useState<UndoState | null>(null);
  const [isSavingLanguage, setIsSavingLanguage] = useState(false);

  const storedLanguageCode = useSettingsStore((s) => s.languageCode);
  const displayCurrency = useSettingsStore((s) => s.displayCurrency);
  const currencyMode = useSettingsStore((s) => s.currencyMode);
  const setFromProfile = useSettingsStore((s) => s.setFromProfile);
  const setLanguageCode = useSettingsStore((s) => s.setLanguageCode);

  const languageCode = normalizeLanguage(storedLanguageCode || i18n.language);

  const languageMutation = useMutation({
    mutationFn: updateLanguage,
    onMutate: (nextLanguage) => {
      const previousLanguage = i18n.language;
      const previousCurrency = displayCurrency;
      const previousMode = currencyMode;
      setIsSavingLanguage(true);
      setLanguageCode(nextLanguage);
      void i18n.changeLanguage(nextLanguage);
      return { previousLanguage, previousCurrency, previousMode };
    },
    onSuccess: (updatedProfile, nextLanguage, context) => {
      const normalizedRequested = normalizeLanguage(nextLanguage);
      const normalizedFromServer = normalizeLanguage(updatedProfile.languageCode);
      const resolvedLanguage =
        normalizedFromServer === normalizedRequested ? normalizedFromServer : normalizedRequested;

      const profileForStore =
        normalizedFromServer === normalizedRequested
          ? updatedProfile
          : {
              ...updatedProfile,
              languageCode: nextLanguage,
            };

      setFromProfile(profileForStore);
      queryClient.setQueryData(profileKeys.me, profileForStore);

      const updatedLanguage = resolvedLanguage;
      if (i18n.language !== updatedLanguage) {
        void i18n.changeLanguage(updatedLanguage);
      }

      if (context?.previousMode === 'AUTO' && context.previousCurrency !== updatedProfile.displayCurrency) {
        setUndoState({
          previousLanguage: context.previousLanguage,
          previousCurrency: context.previousCurrency,
          nextCurrency: updatedProfile.displayCurrency,
        });

        if (mode === 'onboarding') {
          trackOnboardingEvent('currency_changed', {
            currency: updatedProfile.displayCurrency,
          });
        }
      }
    },
    onError: (_error, _nextLanguage, context) => {
      if (context?.previousLanguage) {
        const previousLanguage = normalizeLanguage(context.previousLanguage);
        setLanguageCode(previousLanguage);
        void i18n.changeLanguage(previousLanguage);
      }
      showError(t('common.toast.saveFailed'));
    },
    onSettled: () => {
      setIsSavingLanguage(false);
    },
  });

  const settingsMutation = useMutation({
    mutationFn: updateSettings,
    onSuccess: (updatedProfile) => {
      setFromProfile(updatedProfile);
      queryClient.setQueryData(profileKeys.me, updatedProfile);
    },
    onError: () => {
      showError(t('common.toast.saveFailed'));
    },
  });

  const languageLabel = useMemo(
    () =>
      t(`profile.language.${languageCode}`, {
        defaultValue: languageCode.toUpperCase(),
      }),
    [languageCode, t],
  );

  const currencyLabel = useMemo(() => {
    if (currencyMode === 'AUTO') {
      return `${t('profile.localeCurrency.autoByLanguage')} · ${displayCurrency}`;
    }
    return displayCurrency;
  }, [currencyMode, displayCurrency, t]);

  const isPending = languageMutation.isPending || settingsMutation.isPending || isSavingLanguage;
  const rootClassName = mode === 'onboarding' ? 'am-locale-editor am-locale-editor--onboarding' : 'am-locale-editor';

  const handleLanguageSelect = (nextLanguage: string) => {
    if (nextLanguage === languageCode || isPending) {
      setView('main');
      return;
    }

    if (mode === 'onboarding') {
      trackOnboardingEvent('language_changed', { language: nextLanguage });
    }

    languageMutation.mutate(nextLanguage);
    setView('main');
  };

  const handleSelectAutoMode = () => {
    if (currencyMode === 'AUTO' || isPending) {
      setView('main');
      return;
    }

    if (mode === 'onboarding') {
      trackOnboardingEvent('currency_mode_changed', { mode: 'auto' });
    }

    settingsMutation.mutate(
      { currencyMode: 'AUTO' },
      {
        onSuccess: (updatedProfile) => {
          if (mode === 'onboarding' && updatedProfile.displayCurrency !== displayCurrency) {
            trackOnboardingEvent('currency_changed', {
              currency: updatedProfile.displayCurrency,
            });
          }
        },
      },
    );
    setView('main');
  };

  const handleSelectManualCurrency = (currency: string) => {
    if (isPending) return;
    if (currencyMode === 'MANUAL' && displayCurrency === currency) {
      setView('main');
      return;
    }

    if (mode === 'onboarding') {
      if (currencyMode !== 'MANUAL') {
        trackOnboardingEvent('currency_mode_changed', { mode: 'manual' });
      }
      trackOnboardingEvent('currency_changed', { currency });
    }

    settingsMutation.mutate({
      currencyMode: 'MANUAL',
      displayCurrency: currency,
    });
    setView('main');
  };

  const handleUndo = () => {
    if (!undoState || isPending) return;
    languageMutation.mutate(undoState.previousLanguage);
    setUndoState(null);
  };

  const handleContinue = () => {
    if (mode === 'onboarding') {
      trackOnboardingEvent('locale_continue', {
        language: languageCode,
        currencyMode: modeToAnalytics(currencyMode),
        currency: displayCurrency,
      });
    }
    onContinue?.();
  };

  if (view === 'language') {
    return (
      <div data-testid="locale-currency-editor" className={rootClassName}>
        <Group header={t('profile.language')}>
          {LANGUAGES.map(({ code }) => (
            <GroupItem
              key={code}
              text={t(`profile.language.${code}`)}
              after={languageCode === code ? <Icon name="check" color="accent" /> : undefined}
              onClick={() => handleLanguageSelect(code)}
            />
          ))}
        </Group>
      </div>
    );
  }

  if (view === 'currency') {
    return (
      <div data-testid="locale-currency-editor" className={rootClassName}>
        <Group header={t('profile.currency')}>
          <GroupItem
            text={t('profile.localeCurrency.autoByLanguage')}
            description={t('profile.localeCurrency.autoDescription', {
              currency: displayCurrency,
            })}
            after={currencyMode === 'AUTO' ? <Icon name="check" color="accent" /> : undefined}
            onClick={handleSelectAutoMode}
          />
          {CURRENCIES.map(({ code, labelKey }) => (
            <GroupItem
              key={code}
              text={t(labelKey)}
              after={
                currencyMode === 'MANUAL' && displayCurrency === code ? <Icon name="check" color="accent" /> : undefined
              }
              onClick={() => handleSelectManualCurrency(code)}
            />
          ))}
        </Group>
      </div>
    );
  }

  return (
    <div data-testid="locale-currency-editor" className={rootClassName}>
      {mode === 'onboarding' && (
        <div className="am-locale-editor__intro">
          <Text type="title2" weight="bold">
            {t('onboarding.locale.title')}
          </Text>
          <div className="am-locale-editor__intro-subtitle">
            <Text type="caption1" color="secondary">
              {t('onboarding.locale.subtitle')}
            </Text>
          </div>
        </div>
      )}

      <Group>
        <GroupItem
          text={t('profile.language')}
          after={
            <Text type="body" color="secondary">
              {languageLabel}
            </Text>
          }
          chevron
          onClick={() => setView('language')}
        />
        <GroupItem
          text={t('profile.currency')}
          after={
            <Text type="body" color="secondary">
              {currencyLabel}
            </Text>
          }
          chevron
          onClick={() => setView('currency')}
        />
      </Group>

      {currencyMode === 'MANUAL' && (
        <div className="am-locale-editor__manual">
          <Text type="caption1" color="secondary">
            {t('profile.localeCurrency.manualMicrocopy')}
          </Text>
          <Button
            type="secondary"
            className="am-locale-editor__link"
            disabled={isPending}
            onClick={handleSelectAutoMode}
            text={t('profile.localeCurrency.resetAuto')}
          />
        </div>
      )}

      {undoState && (
        <div className="am-locale-editor__undo">
          <Text type="caption1" color="secondary">
            {t('profile.localeCurrency.autoUpdated', {
              currency: undoState.nextCurrency,
            })}
          </Text>
          <Button
            type="secondary"
            className="am-locale-editor__link"
            onClick={handleUndo}
            disabled={isPending}
            text={t('profile.localeCurrency.undo')}
          />
        </div>
      )}

      {mode === 'onboarding' && (
        <div className="am-locale-editor__cta">
          <Button
            text={t('onboarding.locale.continue')}
            type="primary"
            onClick={handleContinue}
            loading={isPending}
            disabled={isPending}
          />
        </div>
      )}
    </div>
  );
}
