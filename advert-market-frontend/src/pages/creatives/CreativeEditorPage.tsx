import { Text } from '@telegram-tools/ui-kit';
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
import { BackButtonHandler, DeviceFrame, Tappable, TelegramChatSimulator } from '@/shared/ui';
import { fadeIn, pressScale, scaleIn, slideFromLeft, slideFromRight } from '@/shared/ui/animations';
import { SegmentControl } from '@/shared/ui/components/segment-control';

type EditorTab = 'editor' | 'preview';

export default function CreativeEditorPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { creativeId } = useParams<{ creativeId: string }>();

  const isEditing = !!creativeId;
  const { data: creative, isLoading } = useCreativeDetail(creativeId);
  const { data: versions } = useCreativeVersions(creativeId);
  const createMutation = useCreateCreative();
  const updateMutation = useUpdateCreative(creativeId ?? '');

  const [activeTab, setActiveTab] = useState<EditorTab>('editor');
  const [showHistory, setShowHistory] = useState(false);

  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [title, setTitle] = useState(creative?.title ?? '');
  const [text, setText] = useState(creative?.draft.text ?? '');
  const [media, setMedia] = useState<MediaItem[]>(creative?.draft.media ?? []);
  const [buttons, setButtons] = useState<InlineButton[]>(creative?.draft.buttons ?? []);
  const [disableWebPagePreview, setDisableWebPagePreview] = useState(creative?.draft.disableWebPagePreview ?? false);
  const { entities, toggleEntity, isActive } = useEntities(creative?.draft.entities ?? []);

  const isPending = createMutation.isPending || updateMutation.isPending;

  const handleSubmit = useCallback(() => {
    if (!title.trim() || !text.trim()) return;

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

  const previewContent = (
    <DeviceFrame>
      <TelegramChatSimulator text={text} entities={entities} media={media} buttons={filteredButtons} />
    </DeviceFrame>
  );

  return (
    <motion.div {...fadeIn} style={{ paddingBottom: 72 }}>
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
                textareaRef={textareaRef}
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
              textareaRef={textareaRef}
            />
          </div>
          <div style={{ flex: '1 1 40%', minWidth: 0, position: 'sticky', top: 16, alignSelf: 'flex-start' }}>
            {previewContent}
          </div>
        </div>
      </div>

      <div
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          padding: '10px 16px calc(10px + env(safe-area-inset-bottom))',
          background: 'var(--color-background-base)',
          borderTop: '1px solid var(--color-border-separator)',
          zIndex: 10,
        }}
      >
        <motion.button
          {...pressScale}
          type="button"
          disabled={isPending || !title.trim() || !text.trim()}
          onClick={handleSubmit}
          style={{
            width: '100%',
            padding: '10px 16px',
            background: 'var(--color-accent-primary)',
            border: 'none',
            borderRadius: 10,
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 600,
            color: 'var(--color-static-white)',
            WebkitTapHighlightColor: 'transparent',
            opacity: isPending || !title.trim() || !text.trim() ? 0.5 : 1,
          }}
        >
          {isPending ? t('common.loading') : t('common.save')}
        </motion.button>
      </div>

      {versions && (
        <CreativeHistorySheet open={showHistory} onClose={() => setShowHistory(false)} versions={versions} />
      )}
    </motion.div>
  );
}
