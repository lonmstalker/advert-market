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
  type: MediaType;
  fileId: string;
  caption?: string;
  url?: string;
};

export type InlineButton = {
  text: string;
  url: string;
};
