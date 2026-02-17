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
import {
  createChannelPricingRule,
  fetchCategories,
  fetchPostTypes,
  registerChannel,
  updateChannel,
  verifyChannel,
} from '@/features/channels';
import type { ChannelVerifyResponse } from '@/features/channels/types/channel';
import { channelKeys } from '@/shared/api/query-keys';
import { ApiError } from '@/shared/api/types';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { copyToClipboard } from '@/shared/lib/clipboard';
import { parseTonToNano } from '@/shared/lib/ton-format';
import { AppPageShell, AppSectionHeader, AppSurfaceCard, Tappable, TextareaField } from '@/shared/ui';
import { fadeIn, pressScale, slideFromLeft, slideFromRight } from '@/shared/ui/animations';

const BOT_USERNAME = '@AdvertMarketBot';

type PricingRuleDraft = {
  localId: string;
  postType: string | null;
  priceTon: string;
};

type PricingRuleSubmit = {
  name: string;
  postTypes: string[];
  priceNano: number;
  sortOrder: number;
};

function makeDraftId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `rule-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function makeEmptyRuleDraft(): PricingRuleDraft {
  return {
    localId: makeDraftId(),
    postType: null,
    priceTon: '',
  };
}

function looksLikeInviteLink(raw: string): boolean {
  const value = raw.trim().toLowerCase();
  return (
    value.startsWith('+') || value.includes('t.me/+') || value.includes('telegram.me/+') || value.includes('/joinchat/')
  );
}

export default function RegisterChannelPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const haptic = useHaptic();
  const { showSuccess, showError, showInfo } = useToast();

  const [step, setStep] = useState<1 | 2>(1);
  const [username, setUsername] = useState('');
  const [verifyData, setVerifyData] = useState<ChannelVerifyResponse | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [inlineError, setInlineError] = useState<string | null>(null);
  const [ownerNote, setOwnerNote] = useState('');
  const [pricingRules, setPricingRules] = useState<PricingRuleDraft[]>([makeEmptyRuleDraft()]);

  const { data: categories } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
  });
  const { data: postTypes = [] } = useQuery({
    queryKey: channelKeys.postTypes(),
    queryFn: fetchPostTypes,
  });

  useEffect(() => {
    if (postTypes.length === 0) {
      return;
    }
    setPricingRules((prev) =>
      prev.map((rule) =>
        rule.postType
          ? rule
          : {
              ...rule,
              postType: postTypes[0].type,
            },
      ),
    );
  }, [postTypes]);

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
    mutationFn: async (payload: {
      channelId: number;
      categories?: string[];
      ownerNote: string;
      pricingRules: PricingRuleSubmit[];
    }) => {
      let registeredChannelId = payload.channelId;
      try {
        const registered = await registerChannel({
          channelId: payload.channelId,
          categories: payload.categories,
        });
        registeredChannelId = registered.id;
      } catch (error) {
        if (!(error instanceof ApiError && error.status === 409)) {
          throw error;
        }
      }

      for (const rule of payload.pricingRules) {
        await createChannelPricingRule(registeredChannelId, rule);
      }

      await updateChannel(registeredChannelId, {
        ...(payload.categories ? { categories: payload.categories } : {}),
        customRules: payload.ownerNote,
      });
    },
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
    const reference = username.trim();
    if (!reference) return;
    if (looksLikeInviteLink(reference)) {
      setInlineError(t('profile.register.inviteLinkUnsupported'));
      return;
    }
    haptic.impactOccurred('medium');
    setInlineError(null);
    verifyMutation.mutate(reference);
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

    if (pricingRules.length === 0) {
      showError(t('profile.register.pricingRuleInvalid'));
      return;
    }

    const preparedRules: PricingRuleSubmit[] = [];
    const normalizedLang = i18n.language.toLowerCase().startsWith('ru') ? 'ru' : 'en';

    for (const [index, rule] of pricingRules.entries()) {
      if (!rule.postType) {
        showError(t('profile.register.pricingRuleInvalid'));
        return;
      }
      const priceInput = rule.priceTon.trim();
      if (!priceInput) {
        showError(t('profile.register.pricingRuleInvalid'));
        return;
      }

      let priceNano: number;
      try {
        priceNano = Number(parseTonToNano(priceInput));
      } catch {
        showError(t('profile.register.pricingRuleInvalid'));
        return;
      }

      const postType = postTypes.find((item) => item.type === rule.postType);
      const displayName = postType?.labels[normalizedLang] ?? postType?.labels.en ?? rule.postType;

      preparedRules.push({
        name: displayName,
        postTypes: [rule.postType],
        priceNano,
        sortOrder: index,
      });
    }

    registerMutation.mutate({
      channelId: verifyData.channelId,
      categories: selectedCategory ? [selectedCategory] : undefined,
      ownerNote: ownerNote.trim(),
      pricingRules: preparedRules,
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

  const categoryLang = i18n.language.toLowerCase().startsWith('ru') ? 'ru' : 'en';
  const categoryOptions = [
    { label: t('profile.register.categoryPlaceholder'), value: null },
    ...(categories?.map((c) => ({
      label: c.localizedName[categoryLang] ?? c.slug,
      value: c.slug,
    })) ?? []),
  ];

  const postTypeOptions = [
    { label: t('profile.register.postTypePlaceholder'), value: null },
    ...postTypes.map((postType) => ({
      label: postType.labels[categoryLang] ?? postType.labels.en ?? postType.type,
      value: postType.type,
    })),
  ];

  const updateRule = (localId: string, patch: Partial<PricingRuleDraft>) => {
    setPricingRules((prev) => prev.map((rule) => (rule.localId === localId ? { ...rule, ...patch } : rule)));
  };

  return (
    <AppPageShell withTabsPadding={false} testId="profile-register-channel-page">
      <motion.div {...fadeIn} className="flex flex-col min-h-[calc(100vh-40px)] px-4">
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
                  <div className="am-form-field">
                    <Input
                      value={username}
                      onChange={(v) => {
                        setUsername(v);
                        setInlineError(null);
                      }}
                      placeholder={t('profile.register.channelPlaceholder')}
                    />
                  </div>
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
                  <div className="am-form-select">
                    <Select options={categoryOptions} value={selectedCategory} onChange={setSelectedCategory} />
                  </div>
                </div>

                <TextareaField
                  value={ownerNote}
                  onChange={setOwnerNote}
                  label={t('profile.register.ownerNote')}
                  placeholder={t('profile.register.ownerNotePlaceholder')}
                  maxLength={2000}
                  rows={4}
                />

                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <Text type="subheadline2" color="secondary">
                      {t('profile.register.pricingRules')}
                    </Text>
                    <Tappable
                      className="text-accent text-sm font-medium bg-transparent border-none cursor-pointer"
                      onClick={() => setPricingRules((prev) => [...prev, makeEmptyRuleDraft()])}
                    >
                      {t('profile.register.addRule')}
                    </Tappable>
                  </div>

                  <div className="flex flex-col gap-3">
                    {pricingRules.map((rule, index) => (
                      <AppSurfaceCard key={rule.localId}>
                        <div className="p-4 flex flex-col gap-3">
                          <div className="flex items-center justify-between">
                            <Text type="subheadline2" color="secondary">
                              {t('profile.register.ruleLabel', { index: index + 1 })}
                            </Text>
                            {pricingRules.length > 1 && (
                              <Tappable
                                className="text-destructive text-sm font-medium bg-transparent border-none cursor-pointer"
                                onClick={() =>
                                  setPricingRules((prev) => prev.filter((item) => item.localId !== rule.localId))
                                }
                              >
                                {t('profile.register.removeRule')}
                              </Tappable>
                            )}
                          </div>

                          <div>
                            <div className="mb-1">
                              <Text type="caption1" color="secondary">
                                {t('profile.register.postType')}
                              </Text>
                            </div>
                            <div className="am-form-select">
                              <Select
                                options={postTypeOptions}
                                value={rule.postType}
                                onChange={(value) => updateRule(rule.localId, { postType: value })}
                              />
                            </div>
                          </div>

                          <div>
                            <div className="mb-1">
                              <Text type="caption1" color="secondary">
                                {t('profile.register.rulePrice')}
                              </Text>
                            </div>
                            <div className="am-form-field">
                              <Input
                                value={rule.priceTon}
                                onChange={(value) => updateRule(rule.localId, { priceTon: value })}
                                placeholder={t('profile.register.pricePlaceholder')}
                                type="number"
                              />
                            </div>
                          </div>
                        </div>
                      </AppSurfaceCard>
                    ))}
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
