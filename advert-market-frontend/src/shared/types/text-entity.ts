export const TextEntityType = {
  BOLD: 'BOLD',
  ITALIC: 'ITALIC',
  UNDERLINE: 'UNDERLINE',
  STRIKETHROUGH: 'STRIKETHROUGH',
  SPOILER: 'SPOILER',
  CODE: 'CODE',
  PRE: 'PRE',
  TEXT_LINK: 'TEXT_LINK',
} as const;
export type TextEntityType = (typeof TextEntityType)[keyof typeof TextEntityType];

export type TextEntity = {
  type: TextEntityType;
  offset: number;
  length: number;
  url?: string;
  language?: string;
};

export type MediaType = 'PHOTO' | 'VIDEO' | 'GIF' | 'DOCUMENT';

export type MediaItem = {
  id: string;
  type: MediaType;
  url: string;
  thumbnailUrl?: string;
  fileName?: string;
  fileSize?: string;
  mimeType: string;
  sizeBytes: number;
  caption?: string;
  // Legacy transitional field for old mocked payloads.
  fileId?: string;
};

export type InlineButton = {
  id: string;
  text: string;
  url?: string;
};

export type TelegramKeyboardRow = InlineButton[];
