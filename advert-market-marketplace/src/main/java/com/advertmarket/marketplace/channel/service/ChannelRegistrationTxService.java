package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_ALREADY_REGISTERED;

import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction boundary for channel registration DB writes.
 *
 * <p>External I/O (Telegram API) must stay outside of this service.
 */
@Service
@RequiredArgsConstructor
public class ChannelRegistrationTxService {

    private final ChannelRepository channelRepository;

    @Transactional
    @NonNull
    public ChannelResponse registerVerified(@NonNull NewChannel newChannel) {
        long channelId = newChannel.telegramId();
        if (channelRepository.existsByTelegramId(channelId)) {
            throw new DomainException(CHANNEL_ALREADY_REGISTERED,
                    "Channel " + channelId + " is already registered");
        }
        try {
            return channelRepository.insert(newChannel);
        } catch (DuplicateKeyException e) {
            throw new DomainException(CHANNEL_ALREADY_REGISTERED,
                    "Channel " + channelId + " is already registered",
                    e);
        }
    }
}
