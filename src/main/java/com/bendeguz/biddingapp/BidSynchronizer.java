package com.bendeguz.biddingapp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

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
            return durationSinceSpending.getSeconds() > 10;
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
     * Checks whether a campaign specified by its ID is available for bidding. It is available only if the spending
     * on the campaign in the past 10 seconds does not exceed 10 NOK.
     *
     * The referenced campaign MUST be locked before calling this method by lockCampaign.
     * This will ensure that the bidding operations are serializable.
     *
     * @param id The ID of the campaign.
     * @return Whether is the campaign available for spending or not.
     */
    public boolean isCampaignAvailable(long id){
        double totalSpendingInPast10Sec = 0;
        List<Spending> spendings = campaignSpendingMap.get(id);
        spendings.removeIf(Spending::isOlderThan10Sec);
        for(Spending spending : spendings){
            totalSpendingInPast10Sec += spending.getAmount();
        }
        return totalSpendingInPast10Sec < MAXIMUM_SPENDING_PER_CAMPAIGN_PER_10_SEC;
    }

    /**
     * Registers a spending on a campaign specified by its ID.
     *
     * The referenced campaign MUST be locked before calling this method by lockCampaign.
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
