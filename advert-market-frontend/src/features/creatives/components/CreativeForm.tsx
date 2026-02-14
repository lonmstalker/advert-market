import { Button, Input, Text, Toggle } from '@telegram-tools/ui-kit';
import { type RefObject, useCallback, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Textarea } from '@/shared/ui';
import { useEntities } from '../hooks/useEntities';
import type { CreativeDraft, InlineButton, MediaItem, TextEntityType } from '../types/creative';
import { ButtonBuilder } from './ButtonBuilder';
import { FormattingToolbar } from './FormattingToolbar';
import { MediaItemList } from './MediaItemList';

type CreativeFormProps = {
  initialDraft?: CreativeDraft;
  initialTitle?: string;
  onSubmit: (title: string, draft: CreativeDraft) => void;
  isSubmitting?: boolean;
};

const MAX_TEXT_LENGTH = 4096;

export function CreativeForm({ initialDraft, initialTitle, onSubmit, isSubmitting }: CreativeFormProps) {
  const { t } = useTranslation();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [title, setTitle] = useState(initialTitle ?? '');
  const [text, setText] = useState(initialDraft?.text ?? '');
  const [media, setMedia] = useState<MediaItem[]>(initialDraft?.media ?? []);
  const [buttons, setButtons] = useState<InlineButton[]>(initialDraft?.buttons ?? []);
  const [disableWebPagePreview, setDisableWebPagePreview] = useState(initialDraft?.disableWebPagePreview ?? false);

  const { entities, toggleEntity, isActive } = useEntities(initialDraft?.entities ?? []);

  const getSelection = useCallback((): { start: number; end: number } => {
    const ta = textareaRef.current;
    if (!ta) return { start: 0, end: 0 };
    return { start: ta.selectionStart, end: ta.selectionEnd };
  }, []);

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
    [getSelection, toggleEntity],
  );

  const handleLink = useCallback(() => {
    const sel = getSelection();
    if (sel.start === sel.end) return;
    const url = prompt(t('creatives.form.linkPrompt'));
    if (!url) return;
    toggleEntity('TEXT_LINK' as TextEntityType, sel, { url });
    textareaRef.current?.focus();
  }, [getSelection, toggleEntity, t]);

  const activeTypes = useMemo(() => {
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
  }, [getSelection, isActive]);

  const handleSubmit = useCallback(() => {
    if (!title.trim() || !text.trim()) return;
    onSubmit(title.trim(), {
      text,
      entities,
      media,
      buttons: buttons.filter((b) => b.text && b.url),
      disableWebPagePreview,
    });
  }, [title, text, entities, media, buttons, disableWebPagePreview, onSubmit]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <div style={{ marginBottom: 4 }}>
          <Text type="subheadline1" weight="medium">
            {t('creatives.form.title')}
          </Text>
        </div>
        <Input value={title} onChange={setTitle} placeholder={t('creatives.form.titlePlaceholder')} />
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
        <Textarea
          ref={textareaRef}
          value={text}
          onChange={setText}
          placeholder={t('creatives.form.textPlaceholder')}
          maxLength={MAX_TEXT_LENGTH}
          rows={6}
        />
        <div style={{ textAlign: 'right' }}>
          <Text type="caption1" color="secondary">
            {text.length}/{MAX_TEXT_LENGTH}
          </Text>
        </div>
      </div>

      <MediaItemList media={media} onChange={setMedia} />
      <ButtonBuilder buttons={buttons} onChange={setButtons} />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Text type="body">{t('creatives.form.disablePreview')}</Text>
        <Toggle isEnabled={disableWebPagePreview} onChange={setDisableWebPagePreview} />
      </div>

      <Button text={t('common.save')} type="primary" loading={isSubmitting} onClick={handleSubmit} />
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
