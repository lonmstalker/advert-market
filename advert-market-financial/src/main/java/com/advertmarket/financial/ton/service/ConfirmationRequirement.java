package com.advertmarket.financial.ton.service;

/**
 * Result of confirmation policy evaluation.
 *
 * @param confirmations  required number of block confirmations
 * @param operatorReview whether manual operator review is required
 */
public record ConfirmationRequirement(int confirmations, boolean operatorReview) {
}
