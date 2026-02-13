import { useQuery } from '@tanstack/react-query';
import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { fetchChannelDetail, useChannelRights } from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { getMinPrice } from '@/shared/lib/channel-utils';
import { copyToClipboard } from '@/shared/lib/clipboard';
import { computeCpm } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState, PageLoader } from '@/shared/ui';
import { slideUp } from '@/shared/ui/animations';
import { SearchOffIcon } from '@/shared/ui/icons';
import { ChannelDetailCta } from './components/ChannelDetailCta';
import { ChannelDetailHeader } from './components/ChannelDetailHeader';
import { ChannelDetailStats } from './components/ChannelDetailStats';
import { ChannelNextSlot } from './components/ChannelNextSlot';
import { ChannelOpenTelegram } from './components/ChannelOpenTelegram';
import { ChannelOwnerNote } from './components/ChannelOwnerNote';
import { ChannelPricingSection } from './components/ChannelPricingSection';

export default function ChannelDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { channelId } = useParams<{ channelId: string }>();
  const haptic = useHaptic();
  const { showSuccess } = useToast();
  const id = Number(channelId);

  const {
    data: channel,
    isLoading,
    isError,
  } = useQuery({
    queryKey: channelKeys.detail(id),
    queryFn: () => fetchChannelDetail(id),
    enabled: !Number.isNaN(id),
  });

  const { isOwner, isLoading: rightsLoading } = useChannelRights(id);

  if (isLoading || rightsLoading) {
    return (
      <>
        <BackButtonHandler />
        <PageLoader />
      </>
    );
  }

  if (isError || !channel) {
    return (
      <>
        <BackButtonHandler />
        <EmptyState
          icon={<SearchOffIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
          title={t('errors.notFound')}
          description={t('catalog.empty.description')}
          actionLabel={t('common.back')}
          onAction={() => navigate('/catalog')}
        />
      </>
    );
  }

  const minPrice = getMinPrice(channel.pricingRules);
  const heroCpm = minPrice != null && channel.avgReach ? computeCpm(minPrice, channel.avgReach) : null;
  const telegramLink = channel.username ? `https://t.me/${channel.username}` : (channel.inviteLink ?? null);

  const handleShare = async () => {
    const link = `https://t.me/AdvertMarketBot/app?startapp=channel_${channel.id}`;
    const ok = await copyToClipboard(link);
    if (ok) {
      haptic.notificationOccurred('success');
      showSuccess(t('catalog.channel.share'));
    }
  };

  return (
    <>
      <BackButtonHandler />
      <div style={{ paddingBottom: 72 }}>
        <ChannelDetailHeader channel={channel} isOwner={isOwner} onShare={handleShare} />

        {channel.nextAvailableSlot && <ChannelNextSlot nextAvailableSlot={channel.nextAvailableSlot} />}

        <ChannelDetailStats channel={channel} />

        {channel.description && (
          <motion.div {...slideUp} style={{ padding: 16 }}>
            <Text type="subheadline1" color="secondary" style={{ whiteSpace: 'pre-wrap' }}>
              {channel.description}
            </Text>
          </motion.div>
        )}

        {channel.rules?.customRules && <ChannelOwnerNote customRules={channel.rules.customRules} />}

        {telegramLink && <ChannelOpenTelegram link={telegramLink} username={channel.username} />}

        <ChannelPricingSection channel={channel} minPrice={minPrice} heroCpm={heroCpm} />
      </div>

      {!isOwner && <ChannelDetailCta channelId={channel.id} minPrice={minPrice} />}
    </>
  );
}
