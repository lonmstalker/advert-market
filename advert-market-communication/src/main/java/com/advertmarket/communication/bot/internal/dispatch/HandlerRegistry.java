package com.advertmarket.communication.bot.internal.dispatch;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

/**
 * Centralized registry for all Telegram update handlers.
 *
 * <p>Indexes and orders handler lists so that {@link BotDispatcher}
 * depends on a single component for handler discovery.
 * Adding a new handler type requires only a new field and getter
 * here — no change to BotDispatcher's constructor.
 */
@Getter
@Slf4j
@Component
public final class HandlerRegistry {

    private final Map<String, BotCommand> commands;
    private final List<CallbackHandler> callbackHandlers;
    private final List<MessageHandler> messageHandlers;
    private final List<ChatMemberUpdateHandler> chatMemberHandlers;

    /** Collects and indexes all handler beans. */
    public HandlerRegistry(
            @NonNull List<BotCommand> commandList,
            @NonNull List<CallbackHandler> callbackHandlerList,
            @NonNull List<MessageHandler> messageHandlerList,
            @NonNull List<ChatMemberUpdateHandler> chatMemberList) {
        this.commands = commandList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        BotCommand::command,
                        Function.identity()));
        this.callbackHandlers = callbackHandlerList.stream()
                .sorted(Comparator.comparingInt(
                        (CallbackHandler h) -> h.prefix().length())
                        .reversed())
                .toList();
        this.messageHandlers = messageHandlerList.stream()
                .sorted(Comparator.comparingInt(
                        MessageHandler::order))
                .toList();
        this.chatMemberHandlers = List.copyOf(chatMemberList);
        log.info("HandlerRegistry: {} commands, {} callback, "
                + "{} message, {} chat-member handlers",
                commands.size(), callbackHandlers.size(),
                messageHandlers.size(),
                chatMemberHandlers.size());
    }

}