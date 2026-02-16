import { useQuery } from '@tanstack/react-query';
import { AnimatePresence } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { fetchChannelDetail, useChannelRights } from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { getMinPrice } from '@/shared/lib/channel-utils';
import { copyToClipboard } from '@/shared/lib/clipboard';
import { computeCpm } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState, PageLoader, SegmentControl } from '@/shared/ui';
import { SearchOffIcon } from '@/shared/ui/icons';
import { ChannelAboutTab } from './components/ChannelAboutTab';
import { ChannelDetailCta } from './components/ChannelDetailCta';
import { ChannelDetailHeader } from './components/ChannelDetailHeader';
import { ChannelNextSlot } from './components/ChannelNextSlot';
import { ChannelPricingTab } from './components/ChannelPricingTab';
import { ChannelRulesTab } from './components/ChannelRulesTab';

type DetailTab = 'about' | 'pricing' | 'rules';

export default function ChannelDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { channelId } = useParams<{ channelId: string }>();
  const haptic = useHaptic();
  const { showSuccess } = useToast();
  const id = Number(channelId);
  const [activeTab, setActiveTab] = useState<DetailTab>('about');

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
          icon={<SearchOffIcon className="w-7 h-7 text-fg-tertiary" />}
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

  const tabs: { value: DetailTab; label: string }[] = [
    { value: 'about', label: t('catalog.channel.tabAbout') },
    { value: 'pricing', label: t('catalog.channel.tabPricing') },
    { value: 'rules', label: t('catalog.channel.tabRules') },
  ];

  return (
    <>
      <BackButtonHandler />
      <div className="pb-[calc(var(--am-fixed-bottom-bar-base,92px)+var(--am-safe-area-bottom))]">
        <ChannelDetailHeader channel={channel} isOwner={isOwner} onShare={handleShare} />

        {channel.nextAvailableSlot && <ChannelNextSlot nextAvailableSlot={channel.nextAvailableSlot} />}

        <div className="pt-2 px-4 pb-3">
          <SegmentControl tabs={tabs} active={activeTab} onChange={setActiveTab} />
        </div>

        <AnimatePresence mode="wait">
          {activeTab === 'about' && <ChannelAboutTab channel={channel} telegramLink={telegramLink} />}
          {activeTab === 'pricing' && <ChannelPricingTab channel={channel} minPrice={minPrice} heroCpm={heroCpm} />}
          {activeTab === 'rules' && <ChannelRulesTab channel={channel} />}
        </AnimatePresence>
      </div>

      {!isOwner && <ChannelDetailCta channelId={channel.id} minPrice={minPrice} />}
    </>
  );
}
