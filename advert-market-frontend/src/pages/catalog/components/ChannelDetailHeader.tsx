import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import type { ChannelDetail } from '@/features/channels';
import { getChannelLanguages } from '@/shared/lib/channel-utils';
import { formatChannelAge } from '@/shared/lib/time-utils';
import { LanguageBadge } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';
import { ChannelAvatar } from '@/shared/ui/components/channel-avatar';
import { EditIcon, ShareIcon, VerifiedIcon } from '@/shared/ui/icons';

type ChannelDetailHeaderProps = {
  channel: ChannelDetail;
  isOwner: boolean;
  onShare: () => void;
};

export function ChannelDetailHeader({ channel, isOwner, onShare }: ChannelDetailHeaderProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const langs = getChannelLanguages(channel);

  return (
    <motion.div {...fadeIn}>
      <div className="px-4 pt-4 pb-3.5 bg-bg-base border-b border-separator">
        <div className="flex items-start gap-3">
          <ChannelAvatar title={channel.title} size="lg" />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5">
              <Text type="title2" weight="bold">
                <span className="am-truncate">{channel.title}</span>
              </Text>
              {channel.isVerified && (
                <VerifiedIcon
                  className="w-4 h-4 text-accent shrink-0"
                  aria-label={t('catalog.channel.verified')}
                  role="img"
                />
              )}
              {langs.map((code) => (
                <LanguageBadge key={code} code={code} />
              ))}
            </div>
            <Text type="subheadline1" color="secondary">
              <span className="am-truncate">
                {channel.username ? `@${channel.username}` : t('catalog.channel.privateChannel')}
              </span>
            </Text>
            <Text type="caption1" color="tertiary">
              {formatChannelAge(channel.createdAt, t)}
            </Text>
          </div>
          <div className="flex gap-1.5 shrink-0">
            <motion.button
              {...pressScale}
              onClick={onShare}
              className="am-icon-button"
              aria-label={t('catalog.channel.share')}
            >
              <ShareIcon className="w-4 h-4 text-fg-secondary" />
            </motion.button>
            {isOwner && (
              <motion.button
                {...pressScale}
                type="button"
                onClick={() => navigate(`/profile/channels/${channel.id}/edit`)}
                className="am-icon-button"
                aria-label={t('catalog.channel.edit')}
              >
                <EditIcon className="w-4 h-4 text-fg-secondary" />
              </motion.button>
            )}
          </div>
        </div>

        {channel.topics.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-2.5">
            {channel.topics.map((topic) => (
              <span key={topic.slug} className="am-topic-tag">
                {topic.name}
              </span>
            ))}
          </div>
        )}
      </div>
    </motion.div>
  );
}
