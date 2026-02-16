import type { Meta, StoryObj } from '@storybook/react-vite';
import type { InlineButton, MediaItem, TextEntity } from '@/shared/types/text-entity';
import { TelegramPostBody } from '../components/telegram-post-preview/TelegramPostBody';
import { TelegramPostButtons } from '../components/telegram-post-preview/TelegramPostButtons';
import { TelegramPostHeader } from '../components/telegram-post-preview/TelegramPostHeader';
import { TelegramPostMedia } from '../components/telegram-post-preview/TelegramPostMedia';

const postText = 'Escrow-enabled posting in one tap. Open full brief: https://admarket.example/deals/42';

const entities: TextEntity[] = [
  { type: 'BOLD', offset: 0, length: 20 },
  { type: 'TEXT_LINK', offset: 45, length: 34, url: 'https://admarket.example/deals/42' },
];

const buttons: InlineButton[] = [
  { id: 'btn-1', text: 'Open channel', url: 'https://t.me/cryptonewsdaily' },
  { id: 'btn-2', text: 'Create deal', url: 'https://t.me/advert_market_bot/app' },
  { id: 'btn-3', text: 'Share brief', url: 'https://admarket.example/deals/42' },
];

const mediaGrid: MediaItem[] = [
  {
    id: 'media-1',
    type: 'PHOTO',
    url: 'https://placehold.co/640x360/17324d/ffffff?text=Creative+1',
    mimeType: 'image/jpeg',
    sizeBytes: 1024,
    fileId: 'photo-1',
  },
  {
    id: 'media-2',
    type: 'PHOTO',
    url: 'https://placehold.co/640x360/1a4368/ffffff?text=Creative+2',
    mimeType: 'image/jpeg',
    sizeBytes: 2048,
    fileId: 'photo-2',
  },
  {
    id: 'media-3',
    type: 'VIDEO',
    url: 'https://placehold.co/640x360/0d1d2c/ffffff?text=Video+Preview',
    mimeType: 'video/mp4',
    sizeBytes: 4096,
    fileId: 'video-1',
  },
  {
    id: 'media-4',
    type: 'PHOTO',
    url: 'https://placehold.co/640x360/204d75/ffffff?text=Creative+4',
    mimeType: 'image/jpeg',
    sizeBytes: 1024,
    fileId: 'photo-4',
  },
  {
    id: 'media-5',
    type: 'PHOTO',
    url: 'https://placehold.co/640x360/275f8e/ffffff?text=Creative+5',
    mimeType: 'image/jpeg',
    sizeBytes: 1024,
    fileId: 'photo-5',
  },
];

const documentMedia: MediaItem[] = [
  {
    id: 'doc-1',
    type: 'DOCUMENT',
    url: 'https://admarket.example/files/brief.pdf',
    caption: 'Brief v2',
    mimeType: 'application/pdf',
    sizeBytes: 89234,
    fileId: 'doc-file-1',
  },
  {
    id: 'doc-2',
    type: 'DOCUMENT',
    url: 'https://admarket.example/files/creative-spec.md',
    caption: 'Creative spec',
    mimeType: 'text/markdown',
    sizeBytes: 23891,
    fileId: 'doc-file-2',
  },
];

const meta: Meta = {
  title: 'Ad Market/Telegram Preview Parts',
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Header: Story = {
  render: () => (
    <div style={{ display: 'grid', gap: 12 }}>
      <TelegramPostHeader channelTitle="Ad Market News" />
      <TelegramPostHeader
        channelTitle="Crypto Daily"
        channelAvatar="https://placehold.co/80x80/13263a/ffffff?text=CD"
      />
    </div>
  ),
};

export const Body: Story = {
  render: () => <TelegramPostBody text={postText} entities={entities} />,
};

export const Buttons: Story = {
  render: () => <TelegramPostButtons buttons={buttons} />,
};

export const MediaGrid: Story = {
  render: () => <TelegramPostMedia media={mediaGrid} />,
};

export const MediaDocuments: Story = {
  render: () => <TelegramPostMedia media={documentMedia} />,
};
