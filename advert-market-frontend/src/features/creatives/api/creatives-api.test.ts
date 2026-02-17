import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';
import { createCreative, flattenButtons, nonEmptyKeyboardRows, uploadCreativeMedia } from './creatives-api';

const API_BASE = '/api/v1';

describe('creatives-api helpers', () => {
  it('normalizes keyboard rows and drops empty/invalid buttons', () => {
    const rows = [
      [
        { id: 'btn-1', text: 'Open', url: 'https://example.com' },
        { id: '', text: 'Docs', url: 'https://example.com/docs' },
      ],
      [
        { id: 'btn-2', text: '   ', url: 'https://example.com/blank' },
        { id: 'btn-3', text: 'No URL' },
      ],
      [],
    ];

    const normalized = nonEmptyKeyboardRows(rows);

    expect(normalized).toHaveLength(1);
    expect(normalized[0]).toHaveLength(2);
    expect(normalized[0]?.[0]).toMatchObject({ id: 'btn-1', text: 'Open', url: 'https://example.com' });
    expect(normalized[0]?.[1]?.id).toContain('btn-');
    expect(normalized[0]?.[1]?.text).toBe('Docs');
  });

  it('flattens row-based keyboard for legacy UI consumers', () => {
    const rows = [
      [{ id: 'b1', text: 'One', url: 'https://example.com/1' }],
      [
        { id: 'b2', text: 'Two', url: 'https://example.com/2' },
        { id: 'b3', text: 'Three' },
      ],
    ];

    expect(flattenButtons(rows).map((button) => button.id)).toEqual(['b1', 'b2', 'b3']);
  });

  it('creates creative via v2 contract and maps keyboardRows to row-based buttons', async () => {
    const created = await createCreative({
      title: 'Template',
      text: 'Post text',
      entities: [{ type: 'BOLD', offset: 0, length: 4 }],
      media: [],
      buttons: [[{ id: 'b1', text: 'Open', url: 'https://example.com' }]],
      disableWebPagePreview: false,
    });

    expect(created.title).toBe('Template');
    expect(created.draft.buttons).toHaveLength(1);
    expect(created.draft.buttons[0]?.[0]).toMatchObject({
      text: 'Open',
      url: 'https://example.com',
    });
  });

  it('accepts backend keyboard button url as plain string (non-URL)', async () => {
    server.use(
      http.post(`${API_BASE}/creatives`, () => {
        return HttpResponse.json(
          {
            id: 'creative-non-url',
            title: 'Template',
            draft: {
              text: 'Post text',
              entities: [],
              media: [],
              keyboardRows: [[{ id: 'b1', text: 'Open', url: 'Тест' }]],
              disableWebPagePreview: false,
            },
            version: 1,
            createdAt: '2026-02-17T09:02:27.72319Z',
            updatedAt: '2026-02-17T09:02:27.72319Z',
          },
          { status: 201 },
        );
      }),
    );

    const created = await createCreative({
      title: 'Template',
      text: 'Post text',
      entities: [],
      media: [],
      buttons: [[{ id: 'b1', text: 'Open', url: 'https://example.com' }]],
      disableWebPagePreview: false,
    });

    expect(created.draft.buttons[0]?.[0]?.url).toBe('Тест');
  });

  it('uploads media through /creatives/media and returns canonical media asset', async () => {
    const file = new File(['abc'], 'banner.png', { type: 'image/png' });

    const uploaded = await uploadCreativeMedia(file, 'PHOTO');

    expect(uploaded.type).toBe('PHOTO');
    expect(uploaded.mimeType.startsWith('image/')).toBe(true);
    expect(uploaded.sizeBytes).toBeGreaterThan(0);
    expect(
      uploaded.url.startsWith('http://') || uploaded.url.startsWith('https://') || uploaded.url.startsWith('data:'),
    ).toBe(true);
  });

  it('accepts relative media URLs from create response and normalizes them', async () => {
    server.use(
      http.post(`${API_BASE}/creatives`, () => {
        return HttpResponse.json(
          {
            id: 'creative-relative-media',
            title: 'Template',
            draft: {
              text: 'Post text',
              entities: [],
              media: [
                {
                  id: 'media-1',
                  type: 'PHOTO',
                  url: '/creative-media/creatives/photo.png',
                  thumbnailUrl: '/creative-media/creatives/thumb.png',
                  fileName: 'photo.png',
                  fileSize: '10 KB',
                  mimeType: 'image/png',
                  sizeBytes: 10240,
                  caption: 'Preview',
                },
              ],
              keyboardRows: [[{ id: 'b1', text: 'Open', url: 'https://example.com' }]],
              disableWebPagePreview: false,
            },
            version: 1,
            createdAt: '2026-02-17T09:02:27.72319Z',
            updatedAt: '2026-02-17T09:02:27.72319Z',
          },
          { status: 201 },
        );
      }),
    );

    const created = await createCreative({
      title: 'Template',
      text: 'Post text',
      entities: [],
      media: [],
      buttons: [[{ id: 'b1', text: 'Open', url: 'https://example.com' }]],
      disableWebPagePreview: false,
    });

    const expectedUrl = new URL('/creative-media/creatives/photo.png', window.location.origin).toString();
    const expectedThumb = new URL('/creative-media/creatives/thumb.png', window.location.origin).toString();
    expect(created.draft.media[0]?.url).toBe(expectedUrl);
    expect(created.draft.media[0]?.thumbnailUrl).toBe(expectedThumb);
  });

  it('accepts relative media URLs from upload response and normalizes them', async () => {
    server.use(
      http.post(`${API_BASE}/creatives/media`, () => {
        return HttpResponse.json(
          {
            id: 'media-relative',
            type: 'PHOTO',
            url: '/creative-media/creatives/upload-photo.png',
            thumbnailUrl: '/creative-media/creatives/upload-photo-thumb.png',
            fileName: 'upload-photo.png',
            fileSize: '12 KB',
            mimeType: 'image/png',
            sizeBytes: 12000,
            caption: null,
          },
          { status: 201 },
        );
      }),
    );

    const file = new File(['abc'], 'banner.png', { type: 'image/png' });
    const uploaded = await uploadCreativeMedia(file, 'PHOTO');

    expect(uploaded.url).toBe(new URL('/creative-media/creatives/upload-photo.png', window.location.origin).toString());
    expect(uploaded.thumbnailUrl).toBe(
      new URL('/creative-media/creatives/upload-photo-thumb.png', window.location.origin).toString(),
    );
  });
});
