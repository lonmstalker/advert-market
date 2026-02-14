import { Input, Text, Toggle } from '@telegram-tools/ui-kit';
import { type RefObject, useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Textarea } from '@/shared/ui';
import type { InlineButton, MediaItem, TextEntityType } from '../types/creative';
import { ButtonBuilder } from './ButtonBuilder';
import { FormattingToolbar } from './FormattingToolbar';
import { MediaItemList } from './MediaItemList';

type CreativeFormProps = {
  title: string;
  onTitleChange: (title: string) => void;
  text: string;
  onTextChange: (text: string) => void;
  media: MediaItem[];
  onMediaChange: (media: MediaItem[]) => void;
  buttons: InlineButton[];
  onButtonsChange: (buttons: InlineButton[]) => void;
  toggleEntity: (type: TextEntityType, selection: { start: number; end: number }, extra?: { url?: string }) => void;
  isActive: (type: TextEntityType, cursorPos: number) => boolean;
  disableWebPagePreview: boolean;
  onDisableWebPagePreviewChange: (value: boolean) => void;
  textareaRef: RefObject<HTMLTextAreaElement | null>;
};

const MAX_TEXT_LENGTH = 4096;

export function CreativeForm({
  title,
  onTitleChange,
  text,
  onTextChange,
  media,
  onMediaChange,
  buttons,
  onButtonsChange,
  toggleEntity,
  isActive,
  disableWebPagePreview,
  onDisableWebPagePreviewChange,
  textareaRef,
}: CreativeFormProps) {
  const { t } = useTranslation();
  const [isFocused, setIsFocused] = useState(false);

  const getSelection = useCallback((): { start: number; end: number } => {
    const ta = textareaRef.current;
    if (!ta) return { start: 0, end: 0 };
    return { start: ta.selectionStart, end: ta.selectionEnd };
  }, [textareaRef]);

  const hasSelection = useCallback((): boolean => {
    const sel = getSelection();
    return sel.start !== sel.end;
  }, [getSelection]);

  const handleFormat = useCallback(
    (type: TextEntityType) => {
      const sel = getSelection();
      if (sel.start === sel.end) return;
      toggleEntity(type, sel);
      textareaRef.current?.focus();
    },
    [getSelection, toggleEntity, textareaRef],
  );

  const handleLink = useCallback(() => {
    const sel = getSelection();
    if (sel.start === sel.end) return;
    const url = prompt(t('creatives.form.linkPrompt'));
    if (!url) return;
    toggleEntity('TEXT_LINK' as TextEntityType, sel, { url });
    textareaRef.current?.focus();
  }, [getSelection, toggleEntity, t, textareaRef]);

  const activeTypes = useMemo(() => {
    if (!isFocused) return new Set<TextEntityType>();
    const sel = getSelection();
    const cursorPos = sel.start;
    const active = new Set<TextEntityType>();
    const allTypes: TextEntityType[] = [
      'BOLD',
      'ITALIC',
      'UNDERLINE',
      'STRIKETHROUGH',
      'CODE',
      'SPOILER',
      'TEXT_LINK',
    ] as TextEntityType[];
    for (const type of allTypes) {
      if (isActive(type, cursorPos)) active.add(type);
    }
    return active;
  }, [isFocused, getSelection, isActive]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <div style={{ marginBottom: 4 }}>
          <Text type="subheadline1" weight="medium">
            {t('creatives.form.title')}
          </Text>
        </div>
        <Input value={title} onChange={onTitleChange} placeholder={t('creatives.form.titlePlaceholder')} />
      </div>

      <div>
        <div style={{ marginBottom: 4 }}>
          <Text type="subheadline1" weight="medium">
            {t('creatives.form.text')}
          </Text>
        </div>
        <FormattingToolbar
          onFormat={handleFormat}
          onLink={handleLink}
          activeTypes={activeTypes}
          disabled={!hasSelection()}
        />
        <div style={{ position: 'relative' }}>
          <Textarea
            ref={textareaRef}
            value={text}
            onChange={onTextChange}
            placeholder={t('creatives.form.textPlaceholder')}
            maxLength={MAX_TEXT_LENGTH}
            rows={6}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
          />
          <div
            style={{
              position: 'absolute',
              bottom: 8,
              right: 12,
              pointerEvents: 'none',
            }}
          >
            <Text type="caption1" color="secondary">
              {text.length}/{MAX_TEXT_LENGTH}
            </Text>
          </div>
        </div>
      </div>

      <MediaItemList media={media} onChange={onMediaChange} />
      <ButtonBuilder buttons={buttons} onChange={onButtonsChange} />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Text type="body">{t('creatives.form.disablePreview')}</Text>
        <Toggle isEnabled={disableWebPagePreview} onChange={onDisableWebPagePreviewChange} />
      </div>
    </div>
  );
}

export function useCreativeFormData(textareaRef: RefObject<HTMLTextAreaElement | null>) {
  return {
    text: textareaRef.current?.value ?? '',
    selectionStart: textareaRef.current?.selectionStart ?? 0,
    selectionEnd: textareaRef.current?.selectionEnd ?? 0,
  };
}
