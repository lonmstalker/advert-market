package com.advertmarket.marketplace.api.dto.telegram;

/**
 * Status of a chat member in a Telegram channel.
 */
public enum ChatMemberStatus {

    CREATOR,
    ADMINISTRATOR,
    MEMBER,
    RESTRICTED,
    LEFT,
    KICKED
}
