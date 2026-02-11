package com.advertmarket.communication.bot.internal.builder;

import com.pengrad.telegrambot.model.WebAppInfo;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for Telegram inline keyboards.
 *
 * <p>Example usage:
 * <pre>{@code
 * KeyboardBuilder.inline()
 *     .callbackButton("RU", "lang:ru")
 *     .callbackButton("EN", "lang:en")
 *     .row()
 *     .urlButton("Open", "https://example.com")
 *     .build();
 * }</pre>
 */
public final class KeyboardBuilder {

    private final List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    private List<InlineKeyboardButton> currentRow = new ArrayList<>();

    private KeyboardBuilder() {
    }

    /** Creates a new inline keyboard builder. */
    public static KeyboardBuilder inline() {
        return new KeyboardBuilder();
    }

    /** Adds a callback button to the current row. */
    public KeyboardBuilder callbackButton(String text, String data) {
        currentRow.add(
                new InlineKeyboardButton(text).callbackData(data));
        return this;
    }

    /** Adds a URL button to the current row. */
    public KeyboardBuilder urlButton(String text, String url) {
        currentRow.add(new InlineKeyboardButton(text).url(url));
        return this;
    }

    /** Adds a Web App button to the current row. */
    public KeyboardBuilder webAppButton(String text, String webAppUrl) {
        currentRow.add(new InlineKeyboardButton(text)
                .webApp(new WebAppInfo(webAppUrl)));
        return this;
    }

    /** Starts a new row of buttons. */
    public KeyboardBuilder row() {
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
            currentRow = new ArrayList<>();
        }
        return this;
    }

    /** Builds the inline keyboard markup. */
    public InlineKeyboardMarkup build() {
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        var markup = new InlineKeyboardMarkup();
        for (var row : rows) {
            markup.addRow(row.toArray(InlineKeyboardButton[]::new));
        }
        return markup;
    }
}
