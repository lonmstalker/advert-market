import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Input, Select, Spinner, Text } from '@telegram-tools/ui-kit';
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
import { BackButtonHandler, EmptyState } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';

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
  const [message, setMessage] = useState('');

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
    if (!selectedRuleId || !channel) return;
    mutation.mutate({
      channelId: channel.id,
      pricingRuleId: Number(selectedRuleId),
      message: message.trim() || undefined,
    });
  };

  if (isLoading) {
    return (
      <>
        <BackButtonHandler />
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
          <Spinner size="32px" color="accent" />
        </div>
      </>
    );
  }

  if (!channel) {
    return (
      <>
        <BackButtonHandler />
        <EmptyState
          emoji="😔"
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
      label: `${rule.postType} — ${formatTon(rule.priceNano)}`,
      value: String(rule.id),
    })),
  ];

  return (
    <>
      <BackButtonHandler />
      <motion.div
        {...fadeIn}
        style={{
          display: 'flex',
          flexDirection: 'column',
          minHeight: 'calc(100vh - 40px)',
          padding: '0 16px',
        }}
      >
        {/* Form content */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 16, paddingTop: 16 }}>
          <Text type="title1" weight="bold">
            {t('deals.create.title')}
          </Text>

          <ChannelCard channel={channel} />

          <div>
            <Text type="subheadline2" color="secondary" style={{ marginBottom: 8 }}>
              {t('deals.create.postType')}
            </Text>
            <Select options={ruleOptions} value={selectedRuleId} onChange={setSelectedRuleId} />
          </div>

          {selectedRule && (
            <div
              style={{
                textAlign: 'center',
                padding: 16,
                borderRadius: 12,
                background: 'var(--color-background-base)',
                border: '1px solid var(--color-border-separator)',
              }}
            >
              <Text type="subheadline2" color="secondary">
                {t('deals.create.price')}
              </Text>
              <Text type="title1" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(selectedRule.priceNano)}</span>
              </Text>
            </div>
          )}

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <Text type="subheadline2" color="secondary">
                {t('deals.create.message')}
              </Text>
              <Text type="caption1" color="tertiary">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{message.length}/2000</span>
              </Text>
            </div>
            <Input
              value={message}
              onChange={setMessage}
              placeholder={t('deals.create.messagePlaceholder')}
              maxLength={2000}
            />
          </div>
        </div>

        {/* Bottom CTA */}
        <div style={{ flexShrink: 0, paddingBottom: 32, paddingTop: 16 }}>
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
    </>
  );
}
