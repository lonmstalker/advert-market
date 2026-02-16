import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { slideUp } from '@/shared/ui/animations';
import { Tappable } from '@/shared/ui/components/tappable';
import { ArrowRightIcon, TelegramIcon } from '@/shared/ui/icons';

type ChannelOpenTelegramProps = {
  link: string;
  username?: string;
};

export function ChannelOpenTelegram({ link, username }: ChannelOpenTelegramProps) {
  const { t } = useTranslation();

  return (
    <motion.div {...slideUp} className="px-4 pt-2 pb-4">
      <Tappable
        onClick={() => window.open(link, '_blank')}
        aria-label={username ? t('catalog.channel.openInTelegram') : t('catalog.channel.joinChannel')}
        className="flex items-center gap-3 p-3.5 bg-bg-base border border-separator rounded-[14px]"
      >
        <div className="am-icon-circle am-icon-circle--md bg-[color-mix(in_srgb,var(--color-link)_8%,transparent)] shrink-0">
          <TelegramIcon className="w-[18px] h-[18px] text-[var(--color-link)]" />
        </div>
        <div className="flex-1 min-w-0 text-left">
          <Text type="subheadline1" color="accent" weight="medium">
            {username ? t('catalog.channel.openInTelegram') : t('catalog.channel.joinChannel')}
          </Text>
          {username && (
            <Text type="caption1" color="tertiary">
              <span className="block mt-0.5">@{username}</span>
            </Text>
          )}
        </div>
        <ArrowRightIcon className="w-4 h-4 text-[var(--color-link)] opacity-50 shrink-0" />
      </Tappable>
    </motion.div>
  );
}
