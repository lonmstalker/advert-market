import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Input, Select, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router';
import { ChannelCard, createDeal, fetchChannelDetail, fetchPostTypes } from '@/features/channels';
import { useCreatives } from '@/features/creatives';
import { channelKeys, dealKeys } from '@/shared/api/query-keys';
import { ApiError } from '@/shared/api/types';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { formatTon } from '@/shared/lib/ton-format';
import { AppPageShell, AppSurfaceCard, BackButtonHandler, EmptyState, PageLoader, TextareaField } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';
import { SadFaceIcon } from '@/shared/ui/icons';
import { buildPostTypeOptions, resolveCreateDealPricing } from './create-deal.helpers';

export default function CreateDealPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const haptic = useHaptic();
  const { showSuccess, showError } = useToast();

  const channelId = Number(searchParams.get('channelId'));

  const { data: channel, isLoading } = useQuery({
    queryKey: channelKeys.detail(channelId),
    queryFn: () => fetchChannelDetail(channelId),
    enabled: !Number.isNaN(channelId) && channelId > 0,
  });

  const [selectedPostType, setSelectedPostType] = useState<string | null>(null);
  const [customAmountTon, setCustomAmountTon] = useState('');
  const [creativeBrief, setCreativeBrief] = useState('');
  const [selectedCreativeId, setSelectedCreativeId] = useState<string | null>(searchParams.get('creativeId'));

  const { data: creativesData } = useCreatives(100);
  const creatives = useMemo(() => creativesData?.pages.flatMap((page) => page.items) ?? [], [creativesData]);
  const selectedCreative = useMemo(
    () => creatives.find((creative) => creative.id === selectedCreativeId),
    [creatives, selectedCreativeId],
  );

  const { data: postTypes = [] } = useQuery({
    queryKey: channelKeys.postTypes(),
    queryFn: fetchPostTypes,
  });

  const postTypeOptions = useMemo(
    () => buildPostTypeOptions(postTypes, channel?.pricingRules ?? [], i18n.language),
    [postTypes, channel?.pricingRules, i18n.language],
  );
  const selectedPostTypeOption = useMemo(
    () => postTypeOptions.find((option) => option.type === selectedPostType) ?? null,
    [postTypeOptions, selectedPostType],
  );
  const hasRuleForSelectedType =
    selectedPostTypeOption?.pricingRuleId != null && selectedPostTypeOption?.priceNano != null;

  useEffect(() => {
    if (selectedPostType || postTypeOptions.length === 0) {
      return;
    }
    setSelectedPostType(postTypeOptions[0].type);
  }, [selectedPostType, postTypeOptions]);

  const mutation = useMutation({
    mutationFn: createDeal,
    onSuccess: (data) => {
      haptic.notificationOccurred('success');
      showSuccess(t('deals.create.title'));
      queryClient.invalidateQueries({ queryKey: dealKeys.lists() });
      navigate(`/deals/${data.id}`, { replace: true });
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        showError(t('deals.create.error.alreadyExists'));
      } else {
        showError(t('deals.create.error.failed'));
      }
    },
  });

  const handleSubmit = () => {
    if (!selectedPostType || !channel) return;
    let pricing: { amountNano: number; pricingRuleId?: number };
    try {
      pricing = resolveCreateDealPricing({
        selectedPostType,
        options: postTypeOptions,
        customAmountTon,
      });
    } catch {
      showError(t('deals.create.error.invalidAmount'));
      return;
    }
    const creativeId = selectedCreative?.id;
    mutation.mutate({
      channelId: channel.id,
      amountNano: pricing.amountNano,
      pricingRuleId: pricing.pricingRuleId,
      creativeId,
      creativeBrief: creativeId ? undefined : creativeBrief.trim() || undefined,
    });
  };

  if (isLoading) {
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
          onAction={() => navigate(-1)}
        />
      </>
    );
  }

  const selectPostTypeOptions = [
    { label: t('deals.create.selectPostType'), value: null },
    ...postTypeOptions.map((option) => ({
      label:
        option.priceNano != null
          ? `${option.label} — ${formatTon(option.priceNano)}`
          : `${option.label} — ${t('deals.create.noRuleLabel')}`,
      value: option.type,
    })),
  ];

  const creativeOptions = [
    { label: t('deals.create.manualCreative'), value: null },
    ...creatives.map((creative) => ({
      label: creative.title,
      value: creative.id,
    })),
  ];

  return (
    <>
      <BackButtonHandler />
      <AppPageShell withTabsPadding={false} testId="deal-create-page-shell">
        <motion.div {...fadeIn} className="flex flex-col min-h-[calc(100vh-40px)]">
          <div className="flex-1 flex flex-col gap-5">
            <Text type="title1" weight="bold">
              {t('deals.create.title')}
            </Text>

            <ChannelCard channel={channel} />

            <div>
              <div className="mb-2">
                <Text type="subheadline2" color="secondary">
                  {t('deals.create.postType')}
                </Text>
              </div>
              <Select options={selectPostTypeOptions} value={selectedPostType} onChange={setSelectedPostType} />
            </div>

            {!hasRuleForSelectedType && selectedPostType && (
              <div>
                <div className="mb-2">
                  <Text type="subheadline2" color="secondary">
                    {t('deals.create.customAmount')}
                  </Text>
                </div>
                <div className="am-form-field">
                  <Input
                    value={customAmountTon}
                    onChange={setCustomAmountTon}
                    placeholder={t('deals.create.customAmountPlaceholder')}
                    type="number"
                  />
                </div>
                <div className="mt-1">
                  <Text type="caption1" color="secondary">
                    {t('deals.create.customAmountHint')}
                  </Text>
                </div>
              </div>
            )}

            <div>
              <div className="mb-2">
                <Text type="subheadline2" color="secondary">
                  {t('deals.create.creative')}
                </Text>
              </div>
              <Select options={creativeOptions} value={selectedCreativeId} onChange={setSelectedCreativeId} />
            </div>

            {selectedPostTypeOption?.priceNano != null && (
              <AppSurfaceCard>
                <div className="text-center p-4">
                  <Text type="subheadline2" color="secondary">
                    {t('deals.create.price')}
                  </Text>
                  <Text type="title1" weight="bold">
                    <span className="am-tabnum">{formatTon(selectedPostTypeOption.priceNano)}</span>
                  </Text>
                </div>
              </AppSurfaceCard>
            )}

            {selectedCreative ? (
              <AppSurfaceCard>
                <div className="p-4 flex flex-col gap-2">
                  <Text type="subheadline2" color="secondary">
                    {t('deals.create.selectedCreative')}
                  </Text>
                  <Text type="title3" weight="bold">
                    {selectedCreative.title}
                  </Text>
                  <Text type="caption1" color="tertiary">
                    {selectedCreative.draft.text}
                  </Text>
                </div>
              </AppSurfaceCard>
            ) : (
              <TextareaField
                value={creativeBrief}
                onChange={setCreativeBrief}
                label={t('deals.create.message')}
                placeholder={t('deals.create.messagePlaceholder')}
                maxLength={2000}
                rows={4}
              />
            )}
          </div>

          <div className="shrink-0 pb-8 pt-6">
            <motion.div {...pressScale}>
              <Button
                text={
                  selectedPostTypeOption?.priceNano != null
                    ? `${t('deals.create.submit')} · ${formatTon(selectedPostTypeOption.priceNano)}`
                    : t('deals.create.submit')
                }
                type="primary"
                onClick={handleSubmit}
                disabled={!selectedPostType || (!hasRuleForSelectedType && !customAmountTon.trim())}
                loading={mutation.isPending}
              />
            </motion.div>
          </div>
        </motion.div>
      </AppPageShell>
    </>
  );
}
