import type { Meta, StoryObj } from '@storybook/react-vite';
import type { InlineButton, MediaItem, TextEntity } from '@/shared/types/text-entity';
import { TelegramChatSimulator } from '../components/telegram-post-preview/TelegramChatSimulator';
import { TelegramPostPreview } from '../components/telegram-post-preview/TelegramPostPreview';
import { TelegramSpoiler } from '../components/telegram-post-preview/TelegramSpoiler';

const text =
  'Новый пост в каталоге\n\nСделка с escrow и постингом в один клик. Подробности: https://admarket.example/deal/42';

const entities: TextEntity[] = [
  { type: 'BOLD', offset: 0, length: 16 },
  { type: 'TEXT_LINK', offset: 72, length: 32, url: 'https://admarket.example/deal/42' },
];

const media: MediaItem[] = [
  { type: 'PHOTO', fileId: 'photo-1', url: 'https://placehold.co/640x360/0c1623/ffffff?text=Creative+1' },
  { type: 'PHOTO', fileId: 'photo-2', url: 'https://placehold.co/640x360/13253b/ffffff?text=Creative+2' },
  { type: 'VIDEO', fileId: 'video-1', url: 'https://placehold.co/640x360/0a111a/ffffff?text=Video+Preview' },
  { type: 'PHOTO', fileId: 'photo-3', url: 'https://placehold.co/640x360/183654/ffffff?text=Creative+3' },
  { type: 'PHOTO', fileId: 'photo-4', url: 'https://placehold.co/640x360/21486f/ffffff?text=Creative+4' },
];

const buttons: InlineButton[] = [
  { text: 'Открыть канал', url: 'https://t.me/cryptonewsdaily' },
  { text: 'Создать сделку', url: 'https://t.me/advert_market_bot/app' },
];

const meta: Meta = {
  title: 'Ad Market/Telegram Preview',
};

export default meta;
type Story = StoryObj;

export const BubbleWithMedia: Story = {
  render: () => (
    <TelegramPostPreview
      text={text}
      entities={entities}
      media={media}
      buttons={buttons}
      channelTitle="Crypto News Daily"
      views="12.5K"
      edited
      time="13:37"
    />
  ),
};

export const ChatSimulator: Story = {
  render: () => (
    <TelegramChatSimulator
      text={text}
      entities={entities}
      media={media.slice(0, 3)}
      buttons={buttons}
      channelTitle="Ad Market"
      subscriberCount="150K"
    />
  ),
};

export const Spoiler: Story = {
  render: () => <TelegramSpoiler>Скрытый блок текста для предпросмотра</TelegramSpoiler>,
};
