import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
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
import { BackButtonHandler, Tappable, TelegramPostPreview } from '@/shared/ui';
import { fadeIn } from '@/shared/ui/animations';
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

  return (
    <motion.div {...fadeIn} style={{ padding: 16 }}>
      <BackButtonHandler />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
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

      <div style={{ marginBottom: 16 }}>
        <SegmentControl tabs={tabs} active={activeTab} onChange={setActiveTab} />
      </div>

      {activeTab === 'editor' ? (
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
          onSubmit={handleSubmit}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
        />
      ) : (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '16px 0' }}>
          <TelegramPostPreview
            text={text}
            entities={entities}
            media={media}
            buttons={buttons.filter((b) => b.text && b.url)}
          />
        </div>
      )}

      {versions && (
        <CreativeHistorySheet open={showHistory} onClose={() => setShowHistory(false)} versions={versions} />
      )}
    </motion.div>
  );
}
