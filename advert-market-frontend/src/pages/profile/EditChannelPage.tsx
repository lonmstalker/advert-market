import { useMutation, useQuery } from '@tanstack/react-query';
import { Button, Input, Select, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import {
  createChannelPricingRule,
  deleteChannelPricingRule,
  fetchCategories,
  fetchChannelDetail,
  fetchPostTypes,
  updateChannel,
  updateChannelPricingRule,
} from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { parseTonToNano } from '@/shared/lib/ton-format';
import {
  AppPageShell,
  AppSectionHeader,
  AppSurfaceCard,
  BackButtonHandler,
  EmptyState,
  PageLoader,
  Tappable,
  TextareaField,
} from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';
import { SadFaceIcon } from '@/shared/ui/icons';

type EditablePricingRule = {
  localId: string;
  existingRuleId?: number;
  postType: string | null;
  priceTon: string;
};

type SavePricingRule = {
  existingRuleId?: number;
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

function nanoToTonInput(nano: number): string {
  const value = BigInt(nano);
  const whole = value / 1_000_000_000n;
  const fraction = (value % 1_000_000_000n).toString().padStart(9, '0').replace(/0+$/, '');
  return fraction.length > 0 ? `${whole}.${fraction}` : whole.toString();
}

export default function EditChannelPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { channelId } = useParams<{ channelId: string }>();
  const haptic = useHaptic();
  const { showSuccess, showError } = useToast();

  const parsedChannelId = Number(channelId ?? Number.NaN);
  const hasValidChannelId = channelId != null && Number.isFinite(parsedChannelId);

  const { data: channel, isLoading: isChannelLoading } = useQuery({
    queryKey: channelKeys.detail(parsedChannelId),
    queryFn: () => fetchChannelDetail(parsedChannelId),
    enabled: hasValidChannelId,
  });
  const { data: categories } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
  });
  const { data: postTypes = [] } = useQuery({
    queryKey: channelKeys.postTypes(),
    queryFn: fetchPostTypes,
  });

  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [ownerNote, setOwnerNote] = useState('');
  const [rules, setRules] = useState<EditablePricingRule[]>([]);
  const [expandedRuleLocalId, setExpandedRuleLocalId] = useState<string | null>(null);
  const [initialRuleIds, setInitialRuleIds] = useState<number[]>([]);

  useEffect(() => {
    if (!channel) return;
    setSelectedCategory(channel.categories[0] ?? null);
    setOwnerNote(channel.rules?.customRules ?? '');
    const mappedRules = channel.pricingRules
      .slice()
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map((rule) => ({
        localId: makeDraftId(),
        existingRuleId: rule.id,
        postType: rule.postTypes[0] ?? null,
        priceTon: nanoToTonInput(rule.priceNano),
      }));
    const fallbackRule = { localId: makeDraftId(), postType: null, priceTon: '' };
    const nextRules = mappedRules.length > 0 ? mappedRules : [fallbackRule];
    setRules(nextRules);
    setExpandedRuleLocalId(nextRules[0]?.localId ?? null);
    setInitialRuleIds(channel.pricingRules.map((rule) => rule.id));
  }, [channel]);

  useEffect(() => {
    if (postTypes.length === 0) return;
    setRules((prev) =>
      prev.map((rule) =>
        rule.postType
          ? rule
          : {
              ...rule,
              postType: postTypes[0]?.type ?? null,
            },
      ),
    );
  }, [postTypes]);

  useEffect(() => {
    if (rules.length === 0) {
      setExpandedRuleLocalId(null);
      return;
    }
    if (!rules.some((rule) => rule.localId === expandedRuleLocalId)) {
      setExpandedRuleLocalId(rules[0]?.localId ?? null);
    }
  }, [expandedRuleLocalId, rules]);

  const normalizedLang = i18n.language.toLowerCase().startsWith('ru') ? 'ru' : 'en';
  const categoryOptions = useMemo(
    () => [
      { label: t('profile.register.categoryPlaceholder'), value: null },
      ...(categories?.map((category) => ({
        label: category.localizedName[normalizedLang] ?? category.slug,
        value: category.slug,
      })) ?? []),
    ],
    [categories, normalizedLang, t],
  );
  const postTypeOptions = useMemo(
    () => [
      { label: t('profile.register.postTypePlaceholder'), value: null },
      ...postTypes.map((postType) => ({
        label: postType.labels[normalizedLang] ?? postType.labels.en ?? postType.type,
        value: postType.type,
      })),
    ],
    [postTypes, normalizedLang, t],
  );

  const saveMutation = useMutation({
    mutationFn: async (payload: { category: string | null; ownerNote: string; rules: SavePricingRule[] }) => {
      await updateChannel(parsedChannelId, {
        categories: payload.category ? [payload.category] : [],
        customRules: payload.ownerNote,
      });

      const nextExistingIds = new Set<number>();
      for (const [index, rule] of payload.rules.entries()) {
        const request = {
          name: rule.name,
          postTypes: rule.postTypes,
          priceNano: rule.priceNano,
          sortOrder: index,
        };
        if (rule.existingRuleId != null) {
          nextExistingIds.add(rule.existingRuleId);
          await updateChannelPricingRule(parsedChannelId, rule.existingRuleId, request);
        } else {
          await createChannelPricingRule(parsedChannelId, request);
        }
      }

      for (const existingRuleId of initialRuleIds) {
        if (!nextExistingIds.has(existingRuleId)) {
          await deleteChannelPricingRule(parsedChannelId, existingRuleId);
        }
      }
    },
    onSuccess: () => {
      haptic.notificationOccurred('success');
      showSuccess(t('common.toast.saved'));
      navigate(`/catalog/channels/${parsedChannelId}`, { replace: true });
    },
    onError: () => {
      haptic.notificationOccurred('error');
      showError(t('common.error'));
    },
  });

  const handleSave = () => {
    const preparedRules: SavePricingRule[] = [];
    for (const [index, rule] of rules.entries()) {
      if (!rule.postType || !rule.priceTon.trim()) {
        showError(t('profile.register.pricingRuleInvalid'));
        return;
      }

      let priceNano: number;
      try {
        priceNano = Number(parseTonToNano(rule.priceTon.trim()));
      } catch {
        showError(t('profile.register.pricingRuleInvalid'));
        return;
      }

      const postType = postTypes.find((item) => item.type === rule.postType);
      preparedRules.push({
        existingRuleId: rule.existingRuleId,
        name: postType?.labels[normalizedLang] ?? postType?.labels.en ?? rule.postType,
        postTypes: [rule.postType],
        priceNano,
        sortOrder: index,
      });
    }

    saveMutation.mutate({
      category: selectedCategory,
      ownerNote: ownerNote.trim(),
      rules: preparedRules,
    });
  };

  const updateRule = (localId: string, patch: Partial<EditablePricingRule>) => {
    setRules((prev) => prev.map((rule) => (rule.localId === localId ? { ...rule, ...patch } : rule)));
  };

  const resolvePostTypeLabel = (postType: string | null): string => {
    if (!postType) {
      return t('profile.register.postTypePlaceholder');
    }
    const type = postTypes.find((item) => item.type === postType);
    return type?.labels[normalizedLang] ?? type?.labels.en ?? postType;
  };

  if (isChannelLoading) {
    return (
      <>
        <BackButtonHandler />
        <PageLoader />
      </>
    );
  }

  if (!channel) {
    return (
      <>
        <BackButtonHandler />
        <EmptyState
          icon={<SadFaceIcon className="w-7 h-7 text-fg-tertiary" />}
          title={t('errors.notFound')}
          description={t('catalog.empty.description')}
          actionLabel={t('common.back')}
          onAction={() => navigate('/catalog')}
        />
      </>
    );
  }

  return (
    <>
      <BackButtonHandler />
      <AppPageShell withTabsPadding={false} testId="profile-edit-channel-page">
        <motion.div {...fadeIn} className="flex flex-col min-h-[calc(100vh-40px)] px-4">
          <AppSectionHeader title={t('catalog.channel.edit')} />

          <div className="flex-1 flex flex-col gap-5">
            <AppSurfaceCard>
              <div className="p-5">
                <Text type="title2" weight="bold">
                  {channel.title}
                </Text>
                {channel.username && (
                  <Text type="subheadline2" color="secondary">
                    @{channel.username}
                  </Text>
                )}
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
                  onClick={() => {
                    const nextRule = { localId: makeDraftId(), postType: postTypes[0]?.type ?? null, priceTon: '' };
                    setRules((prev) => [...prev, nextRule]);
                    setExpandedRuleLocalId(nextRule.localId);
                  }}
                >
                  {t('profile.register.addRule')}
                </Tappable>
              </div>

              <div className="flex flex-col gap-3">
                {rules.map((rule, index) => (
                  <AppSurfaceCard key={rule.localId}>
                    <div className="p-4">
                      <Tappable
                        data-testid={`edit-channel-rule-toggle-${index}`}
                        aria-expanded={expandedRuleLocalId === rule.localId}
                        onClick={() => setExpandedRuleLocalId(rule.localId)}
                        className="am-collapsible-trigger"
                      >
                        <div className="am-collapsible-trigger__copy">
                          <Text type="subheadline2" color="secondary">
                            {t('profile.register.ruleLabel', { index: index + 1 })}
                          </Text>
                          <Text type="caption1" color="tertiary">
                            {resolvePostTypeLabel(rule.postType)}
                            {rule.priceTon.trim() ? ` · ${rule.priceTon.trim()} TON` : ''}
                          </Text>
                        </div>
                        <span
                          aria-hidden="true"
                          className="am-collapsible-trigger__chevron"
                          data-expanded={expandedRuleLocalId === rule.localId ? 'true' : 'false'}
                        >
                          ˅
                        </span>
                      </Tappable>

                      <AnimatePresence initial={false}>
                        {expandedRuleLocalId === rule.localId && (
                          <motion.div
                            key={`${rule.localId}-content`}
                            initial={{ opacity: 0, height: 0 }}
                            animate={{ opacity: 1, height: 'auto' }}
                            exit={{ opacity: 0, height: 0 }}
                            transition={{ duration: 0.18 }}
                            className="am-collapsible-body"
                          >
                            <div className="flex items-center justify-end">
                              {rules.length > 1 && (
                                <Tappable
                                  className="text-destructive text-sm font-medium bg-transparent border-none cursor-pointer"
                                  onClick={() => {
                                    setRules((prev) => prev.filter((item) => item.localId !== rule.localId));
                                  }}
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
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </div>
                  </AppSurfaceCard>
                ))}
              </div>
            </div>
          </div>

          <div className="shrink-0 pb-8 pt-6">
            <motion.div {...pressScale}>
              <Button
                text={saveMutation.isPending ? t('common.loading') : t('common.save')}
                type="primary"
                onClick={handleSave}
                loading={saveMutation.isPending}
              />
            </motion.div>
          </div>
        </motion.div>
      </AppPageShell>
    </>
  );
}
