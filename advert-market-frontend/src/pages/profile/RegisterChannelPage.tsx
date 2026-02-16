import { useMutation, useQuery } from '@tanstack/react-query';
import {
  offBackButtonClick,
  onBackButtonClick,
  openLink,
  openTelegramLink,
  showBackButton,
} from '@telegram-apps/sdk-react';
import { Button, Input, Select, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { fetchCategories, registerChannel, verifyChannel } from '@/features/channels';
import type { ChannelVerifyResponse } from '@/features/channels/types/channel';
import { channelKeys } from '@/shared/api/query-keys';
import { ApiError } from '@/shared/api/types';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { copyToClipboard } from '@/shared/lib/clipboard';
import { parseTonToNano } from '@/shared/lib/ton-format';
import { AppPageShell, AppSectionHeader, AppSurfaceCard } from '@/shared/ui';
import { fadeIn, pressScale, slideFromLeft, slideFromRight } from '@/shared/ui/animations';

const BOT_USERNAME = '@AdvertMarketBot';

function normalizeUsername(raw: string): string {
  let u = raw.trim();
  if (u.startsWith('https://t.me/')) u = u.slice('https://t.me/'.length);
  if (u.startsWith('http://t.me/')) u = u.slice('http://t.me/'.length);
  if (u.startsWith('@')) u = u.slice(1);
  return u;
}

export default function RegisterChannelPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const haptic = useHaptic();
  const { showSuccess, showError, showInfo } = useToast();

  const [step, setStep] = useState<1 | 2>(1);
  const [username, setUsername] = useState('');
  const [verifyData, setVerifyData] = useState<ChannelVerifyResponse | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [price, setPrice] = useState('');
  const [inlineError, setInlineError] = useState<string | null>(null);

  const { data: categories } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
  });

  const verifyMutation = useMutation({
    mutationFn: (u: string) => verifyChannel(u),
    onSuccess: (data) => {
      if (!data.botStatus.isAdmin) {
        setInlineError(t('profile.register.botNotAdmin'));
        haptic.notificationOccurred('error');
        return;
      }
      setVerifyData(data);
      setInlineError(null);
      haptic.notificationOccurred('success');
      setStep(2);
    },
    onError: (error) => {
      haptic.notificationOccurred('error');
      if (error instanceof ApiError) {
        if (error.status === 404) {
          setInlineError(t('profile.register.channelNotFound'));
        } else if (error.status === 409) {
          showInfo(t('profile.register.alreadySynced'));
          navigate('/profile', { replace: true });
        } else {
          setInlineError(error.message);
        }
      } else {
        setInlineError(t('common.error'));
      }
    },
  });

  const registerMutation = useMutation({
    mutationFn: registerChannel,
    onSuccess: () => {
      haptic.notificationOccurred('success');
      showSuccess(t('profile.register.success'));
      navigate('/profile', { replace: true });
    },
    onError: (error) => {
      haptic.notificationOccurred('error');
      if (error instanceof ApiError && error.status === 409) {
        showInfo(t('profile.register.alreadySynced'));
        navigate('/profile', { replace: true });
      } else {
        showError(t('common.error'));
      }
    },
  });

  const handleVerify = () => {
    const normalized = normalizeUsername(username);
    if (!normalized) return;
    haptic.impactOccurred('medium');
    setInlineError(null);
    verifyMutation.mutate(normalized);
  };

  const handleCopyBot = async () => {
    const ok = await copyToClipboard(BOT_USERNAME);
    if (ok) {
      haptic.impactOccurred('light');
      showInfo(BOT_USERNAME);
    }
  };

  const handleOpenBot = () => {
    const url = `https://t.me/${BOT_USERNAME.slice(1)}`;
    try {
      const [openedInTelegram] = openTelegramLink.ifAvailable(url);
      if (openedInTelegram) return;

      const [openedExternally] = openLink.ifAvailable(url);
      if (openedExternally) return;
    } catch {
      // ignore and fall back
    }

    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const handleRegister = () => {
    if (!verifyData) return;
    haptic.impactOccurred('medium');

    let priceNano: number | undefined;
    if (price.trim()) {
      try {
        priceNano = Number(parseTonToNano(price.trim()));
      } catch {
        showError(t('common.error'));
        return;
      }
    }

    registerMutation.mutate({
      channelId: verifyData.channelId,
      categories: selectedCategory ? [selectedCategory] : undefined,
      pricePerPostNano: priceNano,
    });
  };

  const handleBack = useCallback(() => {
    if (step === 2) {
      setStep(1);
    } else {
      navigate('/profile', { replace: true });
    }
  }, [step, navigate]);

  useEffect(() => {
    showBackButton.ifAvailable();
    onBackButtonClick.ifAvailable(handleBack);
    return () => {
      offBackButtonClick.ifAvailable(handleBack);
    };
  }, [handleBack]);

  const categoryOptions = [
    { label: t('profile.register.categoryPlaceholder'), value: null },
    ...(categories?.map((c) => ({
      label: c.localizedName[t('profile.language') === 'English' ? 'en' : 'ru'] ?? c.slug,
      value: c.slug,
    })) ?? []),
  ];

  return (
    <AppPageShell withTabsPadding={false} testId="profile-register-channel-page">
      <motion.div {...fadeIn} className="flex flex-col min-h-[calc(100vh-40px)]">
        <AppSectionHeader title={t('profile.register.title')} />

        <AnimatePresence mode="wait">
          {step === 1 && (
            <motion.div key="step1" {...slideFromLeft} className="flex-1 flex flex-col">
              <div className="flex-1 flex flex-col gap-5">
                <AppSurfaceCard>
                  <div className="p-5 flex flex-col gap-3.5">
                    <Text type="subheadline2" color="secondary">
                      {t('profile.register.addBotInstruction')}
                    </Text>
                    <Text type="caption1" color="secondary">
                      {t('profile.register.autosyncHint')}
                    </Text>
                    <div className="flex gap-2">
                      <Button text={t('profile.register.copyBot')} type="secondary" onClick={handleCopyBot} />
                      <Button text={t('profile.register.openBot')} type="secondary" onClick={handleOpenBot} />
                    </div>
                  </div>
                </AppSurfaceCard>

                <div>
                  <div className="mb-2">
                    <Text type="subheadline2" color="secondary">
                      {t('profile.register.channelLink')}
                    </Text>
                  </div>
                  <Input
                    value={username}
                    onChange={(v) => {
                      setUsername(v);
                      setInlineError(null);
                    }}
                    placeholder={t('profile.register.channelPlaceholder')}
                  />
                  {inlineError && (
                    <motion.div {...fadeIn} className="mt-2">
                      <div className="text-destructive">
                        <Text type="caption1">{inlineError}</Text>
                      </div>
                    </motion.div>
                  )}
                </div>
              </div>

              <div className="shrink-0 pb-8 pt-6">
                <motion.div {...pressScale}>
                  <Button
                    text={verifyMutation.isPending ? t('profile.register.verifying') : t('profile.register.verify')}
                    type="primary"
                    onClick={handleVerify}
                    disabled={!username.trim()}
                    loading={verifyMutation.isPending}
                  />
                </motion.div>
              </div>
            </motion.div>
          )}

          {step === 2 && verifyData && (
            <motion.div key="step2" {...slideFromRight} className="flex-1 flex flex-col">
              <div className="flex-1 flex flex-col gap-5">
                <AppSurfaceCard>
                  <div className="p-5">
                    <div className="mb-1.5">
                      <Text type="subheadline2" color="secondary">
                        {t('profile.register.channelInfo')}
                      </Text>
                    </div>
                    <Text type="title2" weight="bold">
                      {verifyData.title}
                    </Text>
                    {verifyData.username && (
                      <Text type="subheadline2" color="secondary">
                        @{verifyData.username}
                      </Text>
                    )}
                    <div className="mt-1">
                      <Text type="subheadline2" color="secondary">
                        {t('profile.register.subscribers', { count: verifyData.subscriberCount })}
                      </Text>
                    </div>
                  </div>
                </AppSurfaceCard>

                <div>
                  <div className="mb-2">
                    <Text type="subheadline2" color="secondary">
                      {t('profile.register.category')}
                    </Text>
                  </div>
                  <Select options={categoryOptions} value={selectedCategory} onChange={setSelectedCategory} />
                </div>

                <div>
                  <div className="mb-2">
                    <Text type="subheadline2" color="secondary">
                      {t('profile.register.price')}
                    </Text>
                  </div>
                  <Input
                    value={price}
                    onChange={setPrice}
                    placeholder={t('profile.register.pricePlaceholder')}
                    type="number"
                  />
                  <div className="mt-1">
                    <Text type="caption1" color="secondary">
                      {t('profile.register.priceHint')}
                    </Text>
                  </div>
                </div>
              </div>

              <div className="shrink-0 pb-8 pt-6">
                <motion.div {...pressScale}>
                  <Button
                    text={registerMutation.isPending ? t('profile.register.submitting') : t('profile.register.submit')}
                    type="primary"
                    onClick={handleRegister}
                    loading={registerMutation.isPending}
                  />
                </motion.div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </AppPageShell>
  );
}
