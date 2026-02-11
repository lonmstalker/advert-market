package com.advertmarket.communication.bot.internal.builder;

import java.util.regex.Pattern;

/**
 * Utility for escaping MarkdownV2 special characters in text
 * before sending via Telegram Bot API.
 */
public final class MarkdownV2Util {

    private static final Pattern SPECIAL_CHARS =
            Pattern.compile("([_*\\[\\]()~`>#+\\-=|{}.!])");

    private MarkdownV2Util() {
    }

    /** Escapes all MarkdownV2 special characters in the text. */
    public static String escape(String text) {
        return SPECIAL_CHARS.matcher(text).replaceAll("\\\\$1");
    }
}
