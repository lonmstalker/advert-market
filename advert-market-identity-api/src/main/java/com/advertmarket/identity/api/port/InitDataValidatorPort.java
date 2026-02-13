package com.advertmarket.identity.api.port;

import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.shared.exception.DomainException;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Validates Telegram Mini App initData and extracts user information.
 */
public interface InitDataValidatorPort {

    /**
     * Validates initData and extracts user information.
     *
     * @param initData raw Telegram initData query string
     * @return parsed Telegram user data
     * @throws DomainException if validation fails
     */
    @NonNull TelegramUserData validate(@NonNull String initData);
}
