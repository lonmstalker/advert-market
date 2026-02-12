package com.advertmarket.marketplace.service;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_ALREADY_REGISTERED;

import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the two-step channel registration flow:
 * verify → register.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRegistrationService {

    private final ChannelVerificationService verificationService;
    private final ChannelRepository channelRepository;

    /**
     * Step 1: verifies channel ownership and bot admin status.
     *
     * @param username channel username (without @)
     * @param userId   requesting user's Telegram ID
     * @return verification result for the UI
     */
    @NonNull
    public ChannelVerifyResponse verify(@NonNull String username,
                                        long userId) {
        return verificationService.verify(username, userId);
    }

    /**
     * Step 2: registers a verified channel.
     *
     * <p>Re-verifies bot and user status to prevent race conditions,
     * checks uniqueness, then inserts the channel atomically.
     *
     * @param request registration data
     * @param userId  requesting user's Telegram ID
     * @return the registered channel
     */
    @NonNull
    public ChannelResponse register(
            @NonNull ChannelRegistrationRequest request,
            long userId) {
        long channelId = request.channelId();

        if (channelRepository.existsByTelegramId(channelId)) {
            throw new DomainException(CHANNEL_ALREADY_REGISTERED,
                    "Channel " + channelId + " is already registered");
        }

        var verifyResult = verificationService.verifyBotAndUser(
                verificationService.resolveChannelById(channelId),
                userId);

        log.info("Registering channel {} ({}), owner={}",
                channelId, verifyResult.title(), userId);

        return channelRepository.insert(new NewChannel(
                channelId,
                verifyResult.title(),
                verifyResult.username(),
                null,
                verifyResult.subscriberCount(),
                request.category(),
                request.pricePerPostNano(),
                userId));
    }
}
