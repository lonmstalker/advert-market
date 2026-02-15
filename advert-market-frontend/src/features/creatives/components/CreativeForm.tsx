import { Input, Text, Toggle } from '@telegram-tools/ui-kit';
import { type RefObject, useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Textarea } from '@/shared/ui';
import type { InlineButton, MediaItem, TextEntityType } from '../types/creative';
import { ButtonBuilder } from './ButtonBuilder';
import { FormattingToolbar } from './FormattingToolbar';
import { LinkInputSheet } from './LinkInputSheet';
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
  const [linkSheetOpen, setLinkSheetOpen] = useState(false);
  const [pendingSelection, setPendingSelection] = useState<{ start: number; end: number } | null>(null);

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
    setPendingSelection(sel);
    setLinkSheetOpen(true);
  }, [getSelection]);

  const handleLinkSubmit = useCallback(
    (url: string) => {
      if (!pendingSelection) return;
      toggleEntity('TEXT_LINK' as TextEntityType, pendingSelection, { url });
      setPendingSelection(null);
      textareaRef.current?.focus();
    },
    [pendingSelection, toggleEntity, textareaRef],
  );

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

  const charRatio = text.length / MAX_TEXT_LENGTH;
  const counterColor =
    charRatio > 0.95
      ? 'var(--color-state-destructive)'
      : charRatio > 0.8
        ? 'var(--color-state-warning)'
        : 'var(--color-foreground-tertiary)';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <div style={{ marginBottom: 8 }}>
          <Text type="subheadline2" color="secondary">
            {t('creatives.form.title')}
          </Text>
        </div>
        <Input value={title} onChange={onTitleChange} placeholder={t('creatives.form.titlePlaceholder')} />
      </div>

      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <Text type="subheadline2" color="secondary">
            {t('creatives.form.text')}
          </Text>
          <span
            style={{
              fontSize: 12,
              fontVariantNumeric: 'tabular-nums',
              color: counterColor,
              transition: 'color 0.3s',
            }}
          >
            {text.length}/{MAX_TEXT_LENGTH}
          </span>
        </div>
        <FormattingToolbar
          onFormat={handleFormat}
          onLink={handleLink}
          activeTypes={activeTypes}
          disabled={!hasSelection()}
        />
        {!hasSelection() && isFocused && (
          <Text type="caption2" color="tertiary" style={{ marginTop: 2, marginBottom: 4 }}>
            {t('creatives.form.selectTextHint')}
          </Text>
        )}
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
      </div>

      <MediaItemList media={media} onChange={onMediaChange} />
      <ButtonBuilder buttons={buttons} onChange={onButtonsChange} />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Text type="body">{t('creatives.form.disablePreview')}</Text>
        <Toggle isEnabled={disableWebPagePreview} onChange={onDisableWebPagePreviewChange} />
      </div>

      <LinkInputSheet
        open={linkSheetOpen}
        onClose={() => {
          setLinkSheetOpen(false);
          setPendingSelection(null);
        }}
        onSubmit={handleLinkSubmit}
      />
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
