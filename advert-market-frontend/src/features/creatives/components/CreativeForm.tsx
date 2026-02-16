import { Group, GroupItem, Input, Text, Toggle } from '@telegram-tools/ui-kit';
import { type RefObject, useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Textarea } from '@/shared/ui';
import type { MediaItem, MediaType, TelegramKeyboardRow, TextEntityType } from '../types/creative';
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
  buttons: TelegramKeyboardRow[];
  onButtonsChange: (buttons: TelegramKeyboardRow[]) => void;
  onUploadMedia?: (file: File, mediaType: MediaType) => Promise<MediaItem>;
  onDeleteMedia?: (mediaId: string) => Promise<void>;
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
  onUploadMedia,
  onDeleteMedia,
  toggleEntity,
  isActive,
  disableWebPagePreview,
  onDisableWebPagePreviewChange,
  textareaRef,
}: CreativeFormProps) {
  const { t } = useTranslation();
  const [isFocused, setIsFocused] = useState(false);
  const [selection, setSelection] = useState<{ start: number; end: number }>({ start: 0, end: 0 });
  const [linkSheetOpen, setLinkSheetOpen] = useState(false);
  const [pendingSelection, setPendingSelection] = useState<{ start: number; end: number } | null>(null);

  const syncSelection = useCallback(() => {
    const ta = textareaRef.current;
    if (!ta) {
      setSelection((prev) => (prev.start === 0 && prev.end === 0 ? prev : { start: 0, end: 0 }));
      return;
    }

    const next = { start: ta.selectionStart, end: ta.selectionEnd };
    setSelection((prev) => (prev.start === next.start && prev.end === next.end ? prev : next));
  }, [textareaRef]);

  const hasSelection = selection.start !== selection.end;

  const handleFormat = useCallback(
    (type: TextEntityType) => {
      if (!hasSelection) return;
      toggleEntity(type, selection);
      textareaRef.current?.focus();
    },
    [hasSelection, selection, toggleEntity, textareaRef],
  );

  const handleLink = useCallback(() => {
    if (!hasSelection) return;
    setPendingSelection(selection);
    setLinkSheetOpen(true);
  }, [hasSelection, selection]);

  const handleLinkSubmit = useCallback(
    (url: string) => {
      if (!pendingSelection) return;
      toggleEntity('TEXT_LINK', pendingSelection, { url });
      setPendingSelection(null);
      textareaRef.current?.focus();
    },
    [pendingSelection, toggleEntity, textareaRef],
  );

  const activeTypes = useMemo(() => {
    if (!isFocused) return new Set<TextEntityType>();
    const cursorPos = selection.start;
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
  }, [isFocused, selection.start, isActive]);

  const charRatio = text.length / MAX_TEXT_LENGTH;
  const counterStateClass =
    charRatio > 0.95
      ? 'am-creative-counter am-creative-counter--danger'
      : charRatio > 0.8
        ? 'am-creative-counter am-creative-counter--warning'
        : 'am-creative-counter am-creative-counter--default';

  return (
    <div className="am-creative-form">
      <div className="am-creative-form__field">
        <div className="am-creative-form__label">
          <Text type="subheadline2" color="secondary">
            {t('creatives.form.title')}
          </Text>
        </div>
        <Input value={title} onChange={onTitleChange} placeholder={t('creatives.form.titlePlaceholder')} />
      </div>

      <div className="am-creative-form__field">
        <div className="am-creative-form__field-head">
          <Text type="subheadline2" color="secondary">
            {t('creatives.form.text')}
          </Text>
          <span className={counterStateClass}>
            {text.length}/{MAX_TEXT_LENGTH}
          </span>
        </div>
        <FormattingToolbar
          onFormat={handleFormat}
          onLink={handleLink}
          activeTypes={activeTypes}
          disabled={!hasSelection}
        />
        {!hasSelection && isFocused && (
          <div className="am-creative-form__hint">
            <Text type="caption2" color="tertiary">
              {t('creatives.form.selectTextHint')}
            </Text>
          </div>
        )}
        <Textarea
          ref={textareaRef}
          value={text}
          onChange={onTextChange}
          placeholder={t('creatives.form.textPlaceholder')}
          maxLength={MAX_TEXT_LENGTH}
          rows={6}
          autosize
          className="am-creative-form__textarea"
          onFocus={() => {
            setIsFocused(true);
            syncSelection();
          }}
          onBlur={() => {
            setIsFocused(false);
            setSelection({ start: 0, end: 0 });
          }}
          onSelect={syncSelection}
          onKeyUp={syncSelection}
          onMouseUp={syncSelection}
          onTouchEnd={syncSelection}
        />
      </div>

      <MediaItemList
        media={media}
        onChange={onMediaChange}
        onUploadMedia={onUploadMedia}
        onDeleteMedia={onDeleteMedia}
      />
      <ButtonBuilder buttons={buttons} onChange={onButtonsChange} />

      <Group>
        <GroupItem
          text={t('creatives.form.disablePreview')}
          after={<Toggle isEnabled={disableWebPagePreview} onChange={onDisableWebPagePreviewChange} />}
        />
      </Group>

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
