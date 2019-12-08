package com.bendeguz.biddingapp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

/**
 * This class provides mechanisms to check whether a campaign is available for bidding and to handle concurrency between bids.
 * To ensure that no more spending than {@code MAXIMUM_SPENDING_PER_CAMPAIGN_PER_10_SEC} occurs per 10 sec,
 * the following methods needs to be called in this order:
 * 1. {@code lockCampaign} - to lock the campaign so no other concurrent request can access it
 * 2. {@code isCampaignAvailable} - to check if the previously mentioned limit is not exceeded
 * 3. {@code spendOnCampaign} - if the caller will attempt to spend on the campaign (this attempt will probably succeed).
 * 4. {@code unlockCampaign} - it is <b>crucial</b> to make sure this method gets called, because otherwise the campaign
 *                     will become unavailable for bidding until the application is restarted.
 */
public class BidSynchronizer {
    private static final double MAXIMUM_SPENDING_PER_CAMPAIGN_PER_10_SEC = 10.0;

    private static class Spending{
        private final Instant time;
        private final double amount;

        Spending(Instant time, double amount){
            this.time = time;
            this.amount = amount;
        }

        double getAmount(){
            return amount;
        }

        boolean isOlderThan10Sec(){
            Duration durationSinceSpending = Duration.between(time, Instant.now());
            return durationSinceSpending.compareTo(Duration.ofSeconds(10)) > 0;
        }
    }

    private final ConcurrentMap<Long, List<Spending>> campaignSpendingMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Semaphore> campaignLockMap = new ConcurrentHashMap<>();

    public void lockCampaign(long id) throws InterruptedException{
        campaignSpendingMap.putIfAbsent(id, new ArrayList<>());
        campaignLockMap.putIfAbsent(id, new Semaphore(1));
        campaignLockMap.get(id).acquire();
    }

    public void unlockCampaign(long id) {
        campaignLockMap.get(id).release();
    }

    /**
     * Checks whether a campaign specified by its ID is available for bidding with the specified amount.
     * It is available only if sum of the spending on the campaign in the past 10 seconds and the specified amount
     * does not exceed 10 NOK.
     *
     * The referenced campaign MUST be locked by {@code lockCampaign} before calling this method.
     * This will ensure that the bidding operations are serializable.
     *
     * @param id The ID of the campaign.
     * @param amount The amount of spending.
     * @return whether the campaign is available for spending or not.
     */
    public boolean isCampaignAvailableForSpending(long id, double amount){
        double totalSpendingInPast10Sec = 0;
        List<Spending> spendings = campaignSpendingMap.get(id);
        spendings.removeIf(Spending::isOlderThan10Sec);
        for(Spending spending : spendings){
            totalSpendingInPast10Sec += spending.getAmount();
        }
        return totalSpendingInPast10Sec + amount <= MAXIMUM_SPENDING_PER_CAMPAIGN_PER_10_SEC;
    }

    /**
     * Registers a spending on a campaign specified by its ID.
     *
     * The referenced campaign MUST be locked by {@code lockCampaign} before calling this method.
     * This will ensure that the bidding operations are serializable.
     *
     * @param id The ID of the campaign.
     * @param amount The amount of spending.
     */
    public void spendOnCampaign(long id, double amount){
        List<Spending> spendings = campaignSpendingMap.get(id);
        spendings.add(new Spending(Instant.now(), amount));
    }
}
