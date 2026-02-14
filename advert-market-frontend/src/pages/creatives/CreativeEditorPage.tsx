import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import {
  type CreativeDraft,
  CreativeForm,
  CreativeHistorySheet,
  useCreateCreative,
  useCreativeDetail,
  useCreativeVersions,
  useUpdateCreative,
} from '@/features/creatives';
import { Tappable, TelegramPostPreview } from '@/shared/ui';
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

  // Live preview state
  const [previewText, setPreviewText] = useState('');
  const [previewDraft, setPreviewDraft] = useState<CreativeDraft | null>(null);

  const handleSubmit = useCallback(
    (title: string, draft: CreativeDraft) => {
      const req = {
        title,
        text: draft.text,
        entities: draft.entities,
        media: draft.media,
        buttons: draft.buttons,
        disableWebPagePreview: draft.disableWebPagePreview,
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
    },
    [isEditing, createMutation, updateMutation, navigate],
  );

  if (isEditing && isLoading) {
    return (
      <div style={{ padding: 16, textAlign: 'center' }}>
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
          initialDraft={creative?.draft}
          initialTitle={creative?.title}
          onSubmit={(title, draft) => {
            setPreviewText(draft.text);
            setPreviewDraft(draft);
            handleSubmit(title, draft);
          }}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
        />
      ) : (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '16px 0' }}>
          <TelegramPostPreview
            text={previewDraft?.text ?? creative?.draft.text ?? previewText}
            entities={previewDraft?.entities ?? creative?.draft.entities ?? []}
            media={previewDraft?.media ?? creative?.draft.media ?? []}
            buttons={previewDraft?.buttons ?? creative?.draft.buttons ?? []}
          />
        </div>
      )}

      {versions && (
        <CreativeHistorySheet open={showHistory} onClose={() => setShowHistory(false)} versions={versions} />
      )}
    </motion.div>
  );
}
