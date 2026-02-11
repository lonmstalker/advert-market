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
 * Fluent builder for bot replies.
 *
 * <p>Example usage:
 * <pre>{@code
 * Reply.html(ctx, "<b>Welcome!</b>")
 *     .keyboard(KeyboardBuilder.inline()
 *         .urlButton("Open", url)
 *         .build())
 *     .disablePreview()
 *     .send(sender);
 *
 * Reply.callback(ctx).text("Saved").send(sender);
 * Reply.localized(ctx, i18n, "bot.welcome").send(sender);
 * }</pre>
 */
public final class Reply {

    private Long chatId;
    private String callbackQueryId;
    private String htmlText;
    private String callbackAnswerText;
    private InlineKeyboardMarkup keyboard;
    private boolean noPreview;

    private Reply() {
    }

    /** Creates a reply that sends an HTML message to the chat. */
    public static Reply html(@NonNull UpdateContext ctx,
            @NonNull String text) {
        var reply = new Reply();
        reply.chatId = ctx.chatId();
        reply.htmlText = text;
        return reply;
    }

    /**
     * Creates a localized HTML reply using message bundles.
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
        return html(ctx, i18n.msg(key, lang, args));
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
    public Reply text(String answerText) {
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
        var request = new SendMessage(chatId, htmlText)
                .parseMode(ParseMode.HTML);
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
