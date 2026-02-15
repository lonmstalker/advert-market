import { Button, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import {
  CreativeForm,
  CreativeHistorySheet,
  type InlineButton,
  type MediaItem,
  useCreateCreative,
  useCreativeDetail,
  useCreativeVersions,
  useEntities,
  useUpdateCreative,
} from '@/features/creatives';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { BackButtonHandler, FixedBottomBar, Tappable, TelegramChatSimulator } from '@/shared/ui';
import { fadeIn, pressScale, scaleIn, slideFromLeft, slideFromRight } from '@/shared/ui/animations';
import { SegmentControl } from '@/shared/ui/components/segment-control';

type EditorTab = 'editor' | 'preview';

export default function CreativeEditorPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { creativeId } = useParams<{ creativeId: string }>();
  const haptic = useHaptic();

  const isEditing = !!creativeId;
  const { data: creative, isLoading } = useCreativeDetail(creativeId);
  const { data: versions } = useCreativeVersions(creativeId);
  const createMutation = useCreateCreative();
  const updateMutation = useUpdateCreative(creativeId ?? '');

  const [activeTab, setActiveTab] = useState<EditorTab>('editor');
  const [showHistory, setShowHistory] = useState(false);

  // Mobile and desktop layouts are both mounted; keep refs separate so formatting/selection is bound to the visible textarea.
  const textareaRefMobile = useRef<HTMLTextAreaElement>(null);
  const textareaRefDesktop = useRef<HTMLTextAreaElement>(null);

  const [title, setTitle] = useState(creative?.title ?? '');
  const [text, setText] = useState(creative?.draft.text ?? '');
  const [media, setMedia] = useState<MediaItem[]>(creative?.draft.media ?? []);
  const [buttons, setButtons] = useState<InlineButton[]>(creative?.draft.buttons ?? []);
  const [disableWebPagePreview, setDisableWebPagePreview] = useState(creative?.draft.disableWebPagePreview ?? false);
  const { entities, toggleEntity, isActive } = useEntities(creative?.draft.entities ?? []);

  const isPending = createMutation.isPending || updateMutation.isPending;

  const handleSubmit = useCallback(() => {
    if (!title.trim() || !text.trim()) return;
    haptic.notificationOccurred('success');

    const req = {
      title: title.trim(),
      text,
      entities,
      media,
      buttons: buttons.filter((b) => b.text && b.url),
      disableWebPagePreview,
    };

    if (isEditing) {
      updateMutation.mutate(req, {
        onSuccess: () => navigate('/profile/creatives'),
      });
    } else {
      createMutation.mutate(req, {
        onSuccess: () => navigate('/profile/creatives'),
      });
    }
  }, [
    title,
    text,
    entities,
    media,
    buttons,
    disableWebPagePreview,
    isEditing,
    createMutation,
    updateMutation,
    navigate,
    haptic,
  ]);

  if (isEditing && isLoading) {
    return (
      <div style={{ padding: 16, textAlign: 'center' }}>
        <BackButtonHandler />
        <Text type="body" color="secondary">
          {t('common.loading')}
        </Text>
      </div>
    );
  }

  const tabs = [
    { value: 'editor' as const, label: t('creatives.tabs.editor') },
    { value: 'preview' as const, label: t('creatives.tabs.preview') },
  ];

  const filteredButtons = buttons.filter((b) => b.text && b.url);
  const bottomInset = 'calc(var(--am-fixed-bottom-bar-base, 92px) + var(--am-safe-area-bottom))';

  const previewContent = (
    <div
      style={{
        borderRadius: 14,
        overflow: 'hidden',
        border: '1px solid var(--color-border-separator)',
      }}
    >
      <TelegramChatSimulator text={text} entities={entities} media={media} buttons={filteredButtons} />
    </div>
  );

  return (
    <motion.div
      {...fadeIn}
      style={{
        minHeight: 'calc(100vh - 40px)',
      }}
    >
      <BackButtonHandler />

      <div
        style={{
          padding: '16px 16px 0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Text type="title1" weight="bold">
          {isEditing ? t('creatives.editTitle') : t('creatives.newTitle')}
        </Text>
        {isEditing && versions && versions.length > 0 && (
          <Tappable
            onClick={() => setShowHistory(true)}
            style={{
              border: 'none',
              background: 'transparent',
              color: 'var(--color-accent-primary)',
              cursor: 'pointer',
              fontSize: 14,
            }}
          >
            {t('creatives.history.show')}
          </Tappable>
        )}
      </div>

      {/* Mobile: tab switcher */}
      <div className="creative-editor-mobile" style={{ padding: '0 16px', marginBottom: 16 }}>
        <SegmentControl tabs={tabs} active={activeTab} onChange={setActiveTab} />
      </div>

      {/* Mobile: tabbed content */}
      <div className="creative-editor-mobile" style={{ padding: '0 16px' }}>
        <AnimatePresence mode="wait">
          {activeTab === 'editor' ? (
            <motion.div key="editor" {...slideFromLeft}>
              <CreativeForm
                title={title}
                onTitleChange={setTitle}
                text={text}
                onTextChange={setText}
                media={media}
                onMediaChange={setMedia}
                buttons={buttons}
                onButtonsChange={setButtons}
                toggleEntity={toggleEntity}
                isActive={isActive}
                disableWebPagePreview={disableWebPagePreview}
                onDisableWebPagePreviewChange={setDisableWebPagePreview}
                textareaRef={textareaRefMobile}
              />
            </motion.div>
          ) : (
            <motion.div key="preview" {...slideFromRight}>
              <motion.div {...scaleIn}>{previewContent}</motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Desktop: split view */}
      <div className="creative-editor-desktop" style={{ padding: '0 16px' }}>
        <div style={{ display: 'flex', gap: 24 }}>
          <div style={{ flex: '1 1 60%', minWidth: 0 }}>
            <CreativeForm
              title={title}
              onTitleChange={setTitle}
              text={text}
              onTextChange={setText}
              media={media}
              onMediaChange={setMedia}
              buttons={buttons}
              onButtonsChange={setButtons}
              toggleEntity={toggleEntity}
              isActive={isActive}
              disableWebPagePreview={disableWebPagePreview}
              onDisableWebPagePreviewChange={setDisableWebPagePreview}
              textareaRef={textareaRefDesktop}
            />
          </div>
          <div style={{ flex: '1 1 40%', minWidth: 0, position: 'sticky', top: 16, alignSelf: 'flex-start' }}>
            {previewContent}
          </div>
        </div>
      </div>

      {/* Reserve space for the fixed Save bar so content can scroll above it even on short forms. */}
      <div aria-hidden="true" style={{ height: bottomInset }} />

      <FixedBottomBar>
        <motion.div {...pressScale}>
          <Button
            text={t('common.save')}
            type="primary"
            loading={isPending}
            disabled={isPending || !title.trim() || !text.trim()}
            onClick={handleSubmit}
          />
        </motion.div>
      </FixedBottomBar>

      {versions && (
        <CreativeHistorySheet open={showHistory} onClose={() => setShowHistory(false)} versions={versions} />
      )}
    </motion.div>
  );
}
