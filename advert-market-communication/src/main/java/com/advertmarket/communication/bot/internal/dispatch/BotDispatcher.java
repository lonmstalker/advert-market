package com.advertmarket.communication.bot.internal.dispatch;

import com.advertmarket.communication.bot.internal.block.UserBlockPort;
import com.advertmarket.communication.bot.internal.builder.Reply;
import com.advertmarket.communication.bot.internal.error.BotErrorHandler;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

/**
 * Routes incoming Telegram updates to the appropriate command,
 * callback handler, or message handler.
 */
@Slf4j
@Component
public class BotDispatcher {

    private final Map<String, BotCommand> commands;
    private final List<CallbackHandler> callbackHandlers;
    private final List<MessageHandler> messageHandlers;
    private final List<ChatMemberUpdateHandler> chatMemberHandlers;
    private final TelegramSender sender;
    private final BotErrorHandler errorHandler;
    private final UserBlockPort userBlockPort;
    private final LocalizationService i18n;

    /** Creates the dispatcher with all discovered handlers. */
    public BotDispatcher(
            @NonNull HandlerRegistry handlers,
            @NonNull TelegramSender sender,
            @NonNull BotErrorHandler errorHandler,
            @NonNull UserBlockPort userBlockPort,
            @NonNull LocalizationService i18n) {
        this.commands = handlers.getCommands();
        this.callbackHandlers = handlers.getCallbackHandlers();
        this.messageHandlers = handlers.getMessageHandlers();
        this.chatMemberHandlers =
                handlers.getChatMemberHandlers();
        this.sender = sender;
        this.errorHandler = errorHandler;
        this.userBlockPort = userBlockPort;
        this.i18n = i18n;
    }

    /**
     * Dispatches an update to the matching handler.
     *
     * @param ctx the update context
     */
    public void dispatch(UpdateContext ctx) {
        try {
            if (ctx.isMyChatMemberUpdate()) {
                dispatchChatMemberUpdate(ctx);
                return;
            }
            if (userBlockPort.isBlocked(ctx.userId())) {
                Reply.localized(ctx, i18n, "bot.blocked")
                        .send(sender);
                return;
            }
            if (ctx.isCallbackQuery()) {
                dispatchCallback(ctx);
            } else if (ctx.isTextMessage()) {
                dispatchTextMessage(ctx);
            } else {
                dispatchMessage(ctx);
            }
        } catch (Exception e) {
            errorHandler.handleAndNotify(e,
                    ctx.update().updateId(),
                    ctx.userId(), ctx.languageCode());
        }
    }

    private void dispatchChatMemberUpdate(UpdateContext ctx) {
        var update = ctx.myChatMember();
        if (update == null) {
            return;
        }
        for (var handler : chatMemberHandlers) {
            if (handler.canHandle(update)) {
                log.debug("Dispatching chat member update "
                        + "handler={} update_id={}",
                        handler.getClass().getSimpleName(),
                        ctx.update().updateId());
                handler.handle(update);
                return;
            }
        }
        log.debug("Unhandled my_chat_member update: "
                + "update_id={}", ctx.update().updateId());
    }

    private void dispatchTextMessage(UpdateContext ctx) {
        String text = ctx.messageText();
        if (text != null && text.startsWith("/")) {
            dispatchCommand(ctx, text);
        } else {
            dispatchMessage(ctx);
        }
    }

    private void dispatchCommand(UpdateContext ctx, String text) {
        String commandKey = text.contains(" ")
                ? text.substring(0, text.indexOf(' '))
                : text;
        if (commandKey.contains("@")) {
            commandKey = commandKey.substring(
                    0, commandKey.indexOf('@'));
        }
        var command = commands.get(commandKey);
        if (command != null) {
            log.debug("Dispatching command={} update_id={}",
                    commandKey, ctx.update().updateId());
            command.handle(ctx, sender);
        } else {
            log.debug("Unknown command={}", commandKey);
        }
    }

    private void dispatchCallback(UpdateContext ctx) {
        String data = ctx.callbackData();
        if (data == null) {
            return;
        }
        for (var handler : callbackHandlers) {
            if (data.startsWith(handler.prefix())) {
                log.debug("Dispatching callback prefix={} "
                        + "update_id={}",
                        handler.prefix(),
                        ctx.update().updateId());
                handler.handle(ctx, sender);
                return;
            }
        }
        log.debug("No callback handler for data={}", data);
    }

    private void dispatchMessage(UpdateContext ctx) {
        for (var handler : messageHandlers) {
            if (handler.canHandle(ctx)) {
                log.debug("Dispatching to message handler={} "
                        + "update_id={}",
                        handler.getClass().getSimpleName(),
                        ctx.update().updateId());
                handler.handle(ctx, sender);
                return;
            }
        }
        log.debug("Unhandled update type: update_id={}",
                ctx.update().updateId());
    }
}
