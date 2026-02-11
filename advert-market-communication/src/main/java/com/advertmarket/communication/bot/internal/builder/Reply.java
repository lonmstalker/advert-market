package com.advertmarket.communication.bot.internal.builder;

import com.advertmarket.communication.bot.internal.dispatch.UpdateContext;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import com.pengrad.telegrambot.model.LinkPreviewOptions;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Fluent builder for bot replies using MarkdownV2 parse mode.
 *
 * <p>Example usage:
 * <pre>{@code
 * Reply.text(ctx, "*Welcome\\!*")
 *     .keyboard(KeyboardBuilder.inline()
 *         .urlButton("Open", url)
 *         .build())
 *     .disablePreview()
 *     .send(sender);
 *
 * Reply.callback(ctx).callbackText("Saved").send(sender);
 * Reply.localized(ctx, i18n, "bot.welcome").send(sender);
 * }</pre>
 */
public final class Reply {

    private Long chatId;
    private String callbackQueryId;
    private String text;
    private String callbackAnswerText;
    private InlineKeyboardMarkup keyboard;
    private boolean noPreview;

    private Reply() {
    }

    /**
     * Creates a reply that sends a MarkdownV2 message to the chat.
     */
    public static Reply text(@NonNull UpdateContext ctx,
            @NonNull String markdownV2Text) {
        var reply = new Reply();
        reply.chatId = ctx.chatId();
        reply.text = markdownV2Text;
        return reply;
    }

    /**
     * Creates a localized MarkdownV2 reply using message bundles.
     *
     * @param ctx  the update context
     * @param i18n the localization service
     * @param key  the message key
     * @param args optional message arguments
     * @return a pre-filled Reply
     */
    public static Reply localized(@NonNull UpdateContext ctx,
            @NonNull LocalizationService i18n,
            @NonNull String key, Object... args) {
        String lang = ctx.languageCode() != null
                ? ctx.languageCode() : "ru";
        return text(ctx, i18n.msg(key, lang, args));
    }

    /** Creates a reply that answers a callback query. */
    public static Reply callback(@NonNull UpdateContext ctx) {
        var reply = new Reply();
        var cq = ctx.callbackQuery();
        if (cq == null) {
            throw new IllegalStateException(
                    "No callback query in update");
        }
        reply.callbackQueryId = cq.id();
        return reply;
    }

    /** Sets an inline keyboard for this message reply. */
    public Reply keyboard(InlineKeyboardMarkup markup) {
        this.keyboard = markup;
        return this;
    }

    /** Disables link preview for this message reply. */
    public Reply disablePreview() {
        this.noPreview = true;
        return this;
    }

    /** Sets the text shown in the callback answer toast. */
    public Reply callbackText(String answerText) {
        this.callbackAnswerText = answerText;
        return this;
    }

    /** Sends this reply via the given sender. */
    public void send(@NonNull TelegramSender sender) {
        if (callbackQueryId != null) {
            sendCallback(sender);
        } else {
            sendMessage(sender);
        }
    }

    private void sendCallback(TelegramSender sender) {
        var request = new AnswerCallbackQuery(callbackQueryId);
        if (callbackAnswerText != null) {
            request.text(callbackAnswerText);
        }
        sender.execute(request);
    }

    private void sendMessage(TelegramSender sender) {
        var request = new SendMessage(chatId, text)
                .parseMode(ParseMode.MarkdownV2);
        if (keyboard != null) {
            request.replyMarkup(keyboard);
        }
        if (noPreview) {
            request.linkPreviewOptions(
                    new LinkPreviewOptions().isDisabled(true));
        }
        sender.execute(request, chatId);
    }
}
