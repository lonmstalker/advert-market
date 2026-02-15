package com.advertmarket.identity.mapper;

import com.advertmarket.identity.api.dto.LoginResponse;
import com.advertmarket.identity.api.dto.TelegramUserData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link LoginResponse}.
 */
@Mapper(componentModel = "spring")
public interface LoginResponseMapper {

    /** Maps login inputs into response DTO. */
    @Mapping(target = "accessToken", source = "token")
    @Mapping(target = "expiresIn", source = "expiresInSeconds")
    @Mapping(target = "user", source = "userData")
    LoginResponse toResponse(
            TelegramUserData userData,
            String token,
            long expiresInSeconds);

    /** Maps Telegram user data into response summary. */
    @Mapping(target = "username",
            source = "username",
            qualifiedByName = "orEmpty")
    @Mapping(target = "displayName",
            expression = "java(userData.displayName())")
    LoginResponse.UserSummary toUserSummary(
            TelegramUserData userData);

    /**
     * Returns empty string for {@code null} values.
     */
    @Named("orEmpty")
    static String orEmpty(String value) {
        return value != null ? value : "";
    }
}
