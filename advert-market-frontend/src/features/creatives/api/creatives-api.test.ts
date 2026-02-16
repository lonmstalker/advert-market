import { describe, expect, it } from 'vitest';
import { createCreative, flattenButtons, nonEmptyKeyboardRows, uploadCreativeMedia } from './creatives-api';

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

  it('uploads media through /creatives/media and returns canonical media asset', async () => {
    const file = new File(['abc'], 'banner.png', { type: 'image/png' });

    const uploaded = await uploadCreativeMedia(file, 'PHOTO');

    expect(uploaded.type).toBe('PHOTO');
    expect(uploaded.mimeType.startsWith('image/')).toBe(true);
    expect(uploaded.sizeBytes).toBeGreaterThan(0);
    expect(uploaded.url.startsWith('http://') || uploaded.url.startsWith('https://') || uploaded.url.startsWith('data:')).toBe(
      true,
    );
  });
});
