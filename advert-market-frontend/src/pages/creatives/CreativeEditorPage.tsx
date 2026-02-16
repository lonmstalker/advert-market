import { mainButton } from '@telegram-apps/sdk-react';
import { Button, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import {
  CreativeForm,
  CreativeHistorySheet,
  type MediaItem,
  type MediaType,
  type TelegramKeyboardRow,
  ensureButtonId,
  useCreateCreative,
  useDeleteCreativeMedia,
  useCreativeDetail,
  useCreativeVersions,
  useEntities,
  useUpdateCreative,
  useUploadCreativeMedia,
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
  const uploadMediaMutation = useUploadCreativeMedia();
  const deleteMediaMutation = useDeleteCreativeMedia();

  const [activeTab, setActiveTab] = useState<EditorTab>('editor');
  const [showHistory, setShowHistory] = useState(false);

  // Mobile and desktop layouts are both mounted; keep refs separate so formatting/selection is bound to the visible textarea.
  const textareaRefMobile = useRef<HTMLTextAreaElement>(null);
  const textareaRefDesktop = useRef<HTMLTextAreaElement>(null);

  const [title, setTitle] = useState(creative?.title ?? '');
  const [text, setText] = useState(creative?.draft.text ?? '');
  const [media, setMedia] = useState<MediaItem[]>(creative?.draft.media ?? []);
  const [buttons, setButtons] = useState<TelegramKeyboardRow[]>(creative?.draft.buttons ?? []);
  const [disableWebPagePreview, setDisableWebPagePreview] = useState(creative?.draft.disableWebPagePreview ?? false);
  const { entities, replaceEntities, toggleEntity, isActive } = useEntities(creative?.draft.entities ?? []);

  const isPending = createMutation.isPending || updateMutation.isPending;
  const supportsMainButton = useMemo(() => {
    try {
      return mainButton.setParams.isAvailable();
    } catch {
      return false;
    }
  }, []);

  useEffect(() => {
    if (!creative) return;
    setTitle(creative.title);
    setText(creative.draft.text);
    setMedia(creative.draft.media);
    setButtons(creative.draft.buttons);
    setDisableWebPagePreview(creative.draft.disableWebPagePreview);
    replaceEntities(creative.draft.entities);
  }, [creative, replaceEntities]);

  const validButtonRows = useMemo(
    () =>
      buttons
        .map((row) =>
          row
            .map((button) => ensureButtonId(button))
            .filter((button) => button.text.trim().length > 0 && Boolean(button.url)),
        )
        .filter((row) => row.length > 0),
    [buttons],
  );

  const handleSubmit = useCallback(() => {
    if (!title.trim() || !text.trim()) return;
    haptic.notificationOccurred('success');

    const req = {
      title: title.trim(),
      text,
      entities,
      media,
      buttons: validButtonRows,
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
    validButtonRows,
    disableWebPagePreview,
    isEditing,
    createMutation,
    updateMutation,
    navigate,
    haptic,
  ]);

  useEffect(() => {
    if (!supportsMainButton) return;
    try {
      if (mainButton.mount.isAvailable() && !mainButton.isMounted()) {
        mainButton.mount();
      }
    } catch {
      return;
    }

    const clickHandler = () => handleSubmit();
    mainButton.onClick.ifAvailable(clickHandler);
    mainButton.setParams.ifAvailable({
      text: t('common.save'),
      isVisible: true,
      isEnabled: !isPending && !!title.trim() && !!text.trim(),
      isLoaderVisible: isPending,
    });

    return () => {
      mainButton.offClick.ifAvailable(clickHandler);
      mainButton.setParams.ifAvailable({
        isVisible: false,
        isLoaderVisible: false,
      });
    };
  }, [handleSubmit, isPending, supportsMainButton, t, text, title]);

  const handleUploadMedia = useCallback(
    (file: File, mediaType: MediaType) => uploadMediaMutation.mutateAsync({ file, mediaType }),
    [uploadMediaMutation],
  );

  const handleDeleteMedia = useCallback((mediaId: string) => deleteMediaMutation.mutateAsync(mediaId), [deleteMediaMutation]);

  if (isEditing && isLoading) {
    return (
      <div className="p-4 text-center">
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

  const bottomInset = 'calc(var(--am-fixed-bottom-bar-base, 92px) + var(--am-safe-area-bottom))';

  const previewContent = (
    <div className="rounded-xl overflow-hidden bg-bg-secondary">
      <TelegramChatSimulator text={text} entities={entities} media={media} buttons={validButtonRows} />
    </div>
  );

  return (
    <motion.div
      {...fadeIn}
      className="min-h-[calc(100vh-40px)]"
    >
      <BackButtonHandler />

      <div className="px-4 pt-4 flex justify-between items-center mb-4">
        <Text type="title1" weight="bold">
          {isEditing ? t('creatives.editTitle') : t('creatives.newTitle')}
        </Text>
        {isEditing && versions && versions.length > 0 && (
          <Tappable
            onClick={() => setShowHistory(true)}
            className="border-none bg-transparent text-accent cursor-pointer text-sm"
          >
            {t('creatives.history.show')}
          </Tappable>
        )}
      </div>

      {/* Mobile: tab switcher */}
      <div className="creative-editor-mobile px-4 mb-4">
        <SegmentControl tabs={tabs} active={activeTab} onChange={setActiveTab} />
      </div>

      {/* Mobile: tabbed content */}
      <div className="creative-editor-mobile px-4">
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
                onUploadMedia={handleUploadMedia}
                onDeleteMedia={handleDeleteMedia}
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
      <div className="creative-editor-desktop px-4">
        <div className="flex gap-6">
          <div className="flex-[1_1_60%] min-w-0">
            <CreativeForm
              title={title}
              onTitleChange={setTitle}
              text={text}
              onTextChange={setText}
              media={media}
              onMediaChange={setMedia}
              buttons={buttons}
              onButtonsChange={setButtons}
              onUploadMedia={handleUploadMedia}
              onDeleteMedia={handleDeleteMedia}
              toggleEntity={toggleEntity}
              isActive={isActive}
              disableWebPagePreview={disableWebPagePreview}
              onDisableWebPagePreviewChange={setDisableWebPagePreview}
              textareaRef={textareaRefDesktop}
            />
          </div>
          <div className="flex-[1_1_40%] min-w-0 sticky top-4 self-start">
            {previewContent}
          </div>
        </div>
      </div>

      {!supportsMainButton && (
        <>
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
        </>
      )}

      {versions && (
        <CreativeHistorySheet open={showHistory} onClose={() => setShowHistory(false)} versions={versions} />
      )}
    </motion.div>
  );
}
