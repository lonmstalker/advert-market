import { describe, expect, it } from 'vitest';
import type { MediaItem } from '@/shared/types/text-entity';
import { renderWithProviders, screen } from '@/test/test-utils';
import { TelegramPostMedia } from './TelegramPostMedia';

describe('TelegramPostMedia', () => {
  it('renders image media using thumbnail URL when available', () => {
    const media: MediaItem[] = [
      {
        id: 'media-1',
        type: 'PHOTO',
        url: 'https://cdn.example.com/photo-original.png',
        thumbnailUrl: 'https://cdn.example.com/photo-thumb.png',
        mimeType: 'image/png',
        sizeBytes: 1024,
      },
    ];

    renderWithProviders(<TelegramPostMedia media={media} />);

    const image = screen.getByAltText('Media') as HTMLImageElement;
    expect(image.src).toContain('https://cdn.example.com/photo-thumb.png');
  });

  it('renders image media URL when thumbnail is not provided', () => {
    const media: MediaItem[] = [
      {
        id: 'media-2',
        type: 'PHOTO',
        url: 'https://cdn.example.com/photo.png',
        mimeType: 'image/png',
        sizeBytes: 2048,
      },
    ];

    renderWithProviders(<TelegramPostMedia media={media} />);

    const image = screen.getByAltText('Media') as HTMLImageElement;
    expect(image.src).toContain('https://cdn.example.com/photo.png');
  });
});
