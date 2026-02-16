import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Select, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router';
import { ChannelCard, createDeal, fetchChannelDetail } from '@/features/channels';
import { channelKeys, dealKeys } from '@/shared/api/query-keys';
import { ApiError } from '@/shared/api/types';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { formatTon } from '@/shared/lib/ton-format';
import { AppPageShell, AppSurfaceCard, BackButtonHandler, EmptyState, PageLoader, TextareaField } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';
import { SadFaceIcon } from '@/shared/ui/icons';

export default function CreateDealPage() {
  const { t } = useTranslation();
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

  const [selectedRuleId, setSelectedRuleId] = useState<string | null>(null);
  const [creativeBrief, setCreativeBrief] = useState('');

  const selectedRule = channel?.pricingRules.find((r) => r.id === Number(selectedRuleId));

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
    if (!selectedRuleId || !channel || !selectedRule) return;
    mutation.mutate({
      channelId: channel.id,
      amountNano: selectedRule.priceNano,
      pricingRuleId: Number(selectedRuleId),
      creativeBrief: creativeBrief.trim() || undefined,
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

  const ruleOptions = [
    { label: t('deals.create.selectPostType'), value: null },
    ...channel.pricingRules.map((rule) => ({
      label: `${rule.name} — ${formatTon(rule.priceNano)}`,
      value: String(rule.id),
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
              <Select options={ruleOptions} value={selectedRuleId} onChange={setSelectedRuleId} />
            </div>

            {selectedRule && (
              <AppSurfaceCard>
                <div className="text-center p-4">
                  <Text type="subheadline2" color="secondary">
                    {t('deals.create.price')}
                  </Text>
                  <Text type="title1" weight="bold">
                    <span className="am-tabnum">{formatTon(selectedRule.priceNano)}</span>
                  </Text>
                </div>
              </AppSurfaceCard>
            )}

            <TextareaField
              value={creativeBrief}
              onChange={setCreativeBrief}
              label={t('deals.create.message')}
              placeholder={t('deals.create.messagePlaceholder')}
              maxLength={2000}
              rows={4}
            />
          </div>

          <div className="shrink-0 pb-8 pt-6">
            <motion.div {...pressScale}>
              <Button
                text={
                  selectedRule
                    ? `${t('deals.create.submit')} · ${formatTon(selectedRule.priceNano)}`
                    : t('deals.create.submit')
                }
                type="primary"
                onClick={handleSubmit}
                disabled={!selectedRuleId}
                loading={mutation.isPending}
              />
            </motion.div>
          </div>
        </motion.div>
      </AppPageShell>
    </>
  );
}
