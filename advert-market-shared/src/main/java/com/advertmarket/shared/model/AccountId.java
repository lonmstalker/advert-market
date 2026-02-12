package com.advertmarket.shared.model;

import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * String-based account identifier for the double-entry ledger.
 *
 * <p>Each account ID encodes its {@link AccountType} as a prefix.
 * Singleton accounts have no suffix; parameterized accounts use
 * a colon-separated suffix (e.g., {@code "ESCROW:{dealId}"}).
 */
public record AccountId(@NonNull String value) {

    private static final AccountId PLATFORM_TREASURY_INSTANCE =
            new AccountId("PLATFORM_TREASURY");
    private static final AccountId EXTERNAL_TON_INSTANCE =
            new AccountId("EXTERNAL_TON");
    private static final AccountId NETWORK_FEES_INSTANCE =
            new AccountId("NETWORK_FEES");
    private static final AccountId DUST_WRITEOFF_INSTANCE =
            new AccountId("DUST_WRITEOFF");

    private static final Map<String, AccountType> PREFIX_MAP =
            Map.ofEntries(
                    Map.entry("PLATFORM_TREASURY",
                            AccountType.PLATFORM_TREASURY),
                    Map.entry("EXTERNAL_TON",
                            AccountType.EXTERNAL_TON),
                    Map.entry("NETWORK_FEES",
                            AccountType.NETWORK_FEES),
                    Map.entry("DUST_WRITEOFF",
                            AccountType.DUST_WRITEOFF),
                    Map.entry("ESCROW",
                            AccountType.ESCROW),
                    Map.entry("OWNER_PENDING",
                            AccountType.OWNER_PENDING),
                    Map.entry("COMMISSION",
                            AccountType.COMMISSION),
                    Map.entry("OVERPAYMENT",
                            AccountType.OVERPAYMENT),
                    Map.entry("PARTIAL_DEPOSIT",
                            AccountType.PARTIAL_DEPOSIT),
                    Map.entry("LATE_DEPOSIT",
                            AccountType.LATE_DEPOSIT));

    /**
     * Creates an account identifier.
     *
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public AccountId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "AccountId must not be blank");
        }
    }

    /** Returns the platform treasury singleton account. */
    public static @NonNull AccountId platformTreasury() {
        return PLATFORM_TREASURY_INSTANCE;
    }

    /** Returns the external TON singleton account. */
    public static @NonNull AccountId externalTon() {
        return EXTERNAL_TON_INSTANCE;
    }

    /** Returns the network fees singleton account. */
    public static @NonNull AccountId networkFees() {
        return NETWORK_FEES_INSTANCE;
    }

    /** Returns the dust write-off singleton account. */
    public static @NonNull AccountId dustWriteoff() {
        return DUST_WRITEOFF_INSTANCE;
    }

    /**
     * Creates an escrow account for a deal.
     *
     * @param dealId the deal identifier
     * @return escrow account identifier
     */
    public static @NonNull AccountId escrow(
            @NonNull DealId dealId) {
        return new AccountId("ESCROW:" + dealId.value());
    }

    /**
     * Creates an owner pending account for a user.
     *
     * @param userId the user identifier
     * @return owner pending account identifier
     */
    public static @NonNull AccountId ownerPending(
            @NonNull UserId userId) {
        return new AccountId(
                "OWNER_PENDING:" + userId.value());
    }

    /**
     * Creates a commission account for a deal.
     *
     * @param dealId the deal identifier
     * @return commission account identifier
     */
    public static @NonNull AccountId commission(
            @NonNull DealId dealId) {
        return new AccountId("COMMISSION:" + dealId.value());
    }

    /**
     * Creates an overpayment account for a deal.
     *
     * @param dealId the deal identifier
     * @return overpayment account identifier
     */
    public static @NonNull AccountId overpayment(
            @NonNull DealId dealId) {
        return new AccountId("OVERPAYMENT:" + dealId.value());
    }

    /**
     * Creates a partial deposit account for a deal.
     *
     * @param dealId the deal identifier
     * @return partial deposit account identifier
     */
    public static @NonNull AccountId partialDeposit(
            @NonNull DealId dealId) {
        return new AccountId(
                "PARTIAL_DEPOSIT:" + dealId.value());
    }

    /**
     * Creates a late deposit account for a deal.
     *
     * @param dealId the deal identifier
     * @return late deposit account identifier
     */
    public static @NonNull AccountId lateDeposit(
            @NonNull DealId dealId) {
        return new AccountId(
                "LATE_DEPOSIT:" + dealId.value());
    }

    /**
     * Resolves the account type from this identifier's prefix.
     *
     * @return the account type
     * @throws IllegalStateException if the prefix is unknown
     */
    public @NonNull AccountType type() {
        int colonIdx = value.indexOf(':');
        String prefix = colonIdx < 0
                ? value
                : value.substring(0, colonIdx);
        AccountType accountType = PREFIX_MAP.get(prefix);
        if (accountType == null) {
            throw new IllegalStateException(
                    "Unknown account prefix: " + prefix);
        }
        return accountType;
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
