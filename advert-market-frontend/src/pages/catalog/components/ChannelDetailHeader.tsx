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
      <div
        style={{
          padding: '16px 16px 14px',
          background: 'var(--color-background-base)',
          borderBottom: '1px solid var(--color-border-separator)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
          <ChannelAvatar title={channel.title} size="lg" />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Text type="title2" weight="bold">
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                  {channel.title}
                </span>
              </Text>
              {channel.isVerified && (
                <VerifiedIcon
                  style={{ width: 16, height: 16, color: 'var(--color-accent-primary)', flexShrink: 0 }}
                  title={t('catalog.channel.verified')}
                />
              )}
              {langs.map((code) => (
                <LanguageBadge key={code} code={code} />
              ))}
            </div>
            <Text type="subheadline1" color="secondary">
              <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {channel.username ? `@${channel.username}` : t('catalog.channel.privateChannel')}
                <span style={{ color: 'var(--color-foreground-tertiary)' }}>
                  {' \u00b7 '}
                  {formatChannelAge(channel.createdAt, t)}
                </span>
              </span>
            </Text>
          </div>
          <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
            <motion.button
              {...pressScale}
              onClick={onShare}
              style={iconButtonStyle}
              aria-label={t('catalog.channel.share')}
            >
              <ShareIcon style={{ width: 16, height: 16, color: 'var(--color-foreground-secondary)' }} />
            </motion.button>
            {isOwner && (
              <motion.button
                {...pressScale}
                type="button"
                onClick={() => navigate(`/profile/channels/${channel.id}/edit`)}
                style={iconButtonStyle}
                aria-label={t('catalog.channel.edit')}
              >
                <EditIcon style={{ width: 16, height: 16, color: 'var(--color-foreground-secondary)' }} />
              </motion.button>
            )}
          </div>
        </div>

        {channel.topics.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 10 }}>
            {channel.topics.map((topic) => (
              <span
                key={topic.slug}
                style={{
                  padding: '3px 10px',
                  borderRadius: 10,
                  background: 'var(--color-background-secondary)',
                  fontSize: 12,
                  fontWeight: 500,
                  color: 'var(--color-foreground-secondary)',
                }}
              >
                {topic.name}
              </span>
            ))}
          </div>
        )}
      </div>
    </motion.div>
  );
}

const iconButtonStyle: React.CSSProperties = {
  width: 36,
  height: 36,
  borderRadius: 10,
  border: '1px solid var(--color-border-separator)',
  background: 'var(--color-background-secondary)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer',
  WebkitTapHighlightColor: 'transparent',
  padding: 0,
};
