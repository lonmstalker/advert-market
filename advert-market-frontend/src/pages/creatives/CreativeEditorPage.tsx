import { mainButton } from '@telegram-apps/sdk-react';
import { Button, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import {
  CreativeForm,
  CreativeHistorySheet,
  ensureButtonId,
  findFirstInvalidButtonUrl,
  type MediaItem,
  type MediaType,
  type TelegramKeyboardRow,
  useCreateCreative,
  useCreativeDetail,
  useCreativeVersions,
  useDeleteCreativeMedia,
  useEntities,
  useUpdateCreative,
  useUploadCreativeMedia,
} from '@/features/creatives';
import { ApiError } from '@/shared/api/types';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { AppPageShell, BackButtonHandler, FixedBottomBar, Tappable, TelegramChatSimulator } from '@/shared/ui';
import { fadeIn, pressScale, scaleIn, slideFromLeft, slideFromRight } from '@/shared/ui/animations';
import { SegmentControl } from '@/shared/ui/components/segment-control';

type EditorTab = 'editor' | 'preview';

function resolveSaveErrorMessage(error: unknown, fallbackMessage: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return fallbackMessage;
}

export default function CreativeEditorPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { creativeId } = useParams<{ creativeId: string }>();
  const haptic = useHaptic();
  const { showError, showSuccess } = useToast();

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
            .filter((button) => button.text.trim().length > 0 && Boolean(button.url?.trim())),
        )
        .filter((row) => row.length > 0),
    [buttons],
  );
  const hasInvalidButtonUrl = useMemo(() => findFirstInvalidButtonUrl(buttons) !== null, [buttons]);

  const handleSubmit = useCallback(() => {
    if (!title.trim() || !text.trim()) return;
    if (hasInvalidButtonUrl) {
      haptic.notificationOccurred('error');
      showError(t('creatives.form.linkInvalid'));
      return;
    }

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
        onSuccess: () => {
          haptic.notificationOccurred('success');
          showSuccess(t('creatives.toast.saved'));
          navigate('/profile/creatives');
        },
        onError: (error) => {
          haptic.notificationOccurred('error');
          showError(resolveSaveErrorMessage(error, t('common.toast.saveFailed')));
        },
      });
    } else {
      createMutation.mutate(req, {
        onSuccess: () => {
          haptic.notificationOccurred('success');
          showSuccess(t('creatives.toast.created'));
          navigate('/profile/creatives');
        },
        onError: (error) => {
          haptic.notificationOccurred('error');
          showError(resolveSaveErrorMessage(error, t('common.toast.saveFailed')));
        },
      });
    }
  }, [
    title,
    text,
    entities,
    media,
    hasInvalidButtonUrl,
    validButtonRows,
    disableWebPagePreview,
    isEditing,
    createMutation,
    updateMutation,
    navigate,
    haptic,
    showError,
    showSuccess,
    t,
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
      isEnabled: !isPending && !hasInvalidButtonUrl && !!title.trim() && !!text.trim(),
      isLoaderVisible: isPending,
    });

    return () => {
      mainButton.offClick.ifAvailable(clickHandler);
      mainButton.setParams.ifAvailable({
        isVisible: false,
        isLoaderVisible: false,
      });
    };
  }, [handleSubmit, hasInvalidButtonUrl, isPending, supportsMainButton, t, text, title]);

  const handleUploadMedia = useCallback(
    (file: File, mediaType: MediaType) => uploadMediaMutation.mutateAsync({ file, mediaType }),
    [uploadMediaMutation],
  );

  const handleDeleteMedia = useCallback(
    (mediaId: string) => deleteMediaMutation.mutateAsync(mediaId),
    [deleteMediaMutation],
  );

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

  const previewContent = (
    <div className="am-creative-editor__preview-surface">
      <TelegramChatSimulator text={text} entities={entities} media={media} buttons={validButtonRows.flat()} />
    </div>
  );

  return (
    <>
      <BackButtonHandler />
      <AppPageShell withTabsPadding={false} testId="creative-editor-page-shell">
        <motion.div {...fadeIn} className="am-creative-editor-page">
          <div className="am-creative-editor__header">
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

          <div className="creative-editor-mobile am-creative-editor__mobile-tabs">
            <SegmentControl tabs={tabs} active={activeTab} onChange={setActiveTab} />
          </div>

          <div className="creative-editor-mobile am-creative-editor__mobile-content">
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

          <div className="creative-editor-desktop am-creative-editor__desktop">
            <div className="am-creative-editor__desktop-grid">
              <div className="am-creative-editor__desktop-form">
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
              <div className="am-creative-editor__desktop-preview">{previewContent}</div>
            </div>
          </div>

          {!supportsMainButton && <div aria-hidden="true" className="am-fixed-bottom-bar-spacer" />}
        </motion.div>
      </AppPageShell>

      {!supportsMainButton && (
        <FixedBottomBar>
          <motion.div {...pressScale}>
            <Button
              text={t('common.save')}
              type="primary"
              loading={isPending}
              disabled={isPending || hasInvalidButtonUrl || !title.trim() || !text.trim()}
              onClick={handleSubmit}
            />
          </motion.div>
        </FixedBottomBar>
      )}

      {versions && (
        <CreativeHistorySheet open={showHistory} onClose={() => setShowHistory(false)} versions={versions} />
      )}
    </>
  );
}
