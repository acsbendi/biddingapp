package com.bendeguz.biddingapp;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.Java6Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
class BidSynchronizerTest {

    private BidSynchronizer bidSynchronizer;

    @BeforeEach
    void setUp() {
        bidSynchronizer = new BidSynchronizer();
    }

    @Test
    void lock(){
        long campaignId = 1;
        Thread thread1 = new Thread(() -> assertThatCode(() -> {
            bidSynchronizer.lockCampaign(campaignId);
            Thread.sleep(500);
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException());
        Thread thread2 = new Thread(() -> assertThatCode(() -> {
            Instant beforeLockAcquired = Instant.now();
            bidSynchronizer.lockCampaign(campaignId);
            Instant afterLockAcquired = Instant.now();
            Duration lockAcquisitionDuration = Duration.between(beforeLockAcquired, afterLockAcquired);
            assertThat(lockAcquisitionDuration).isGreaterThan(Duration.ofMillis(400));
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException());
        thread1.start();
        assertThatCode(() -> Thread.sleep(50)).doesNotThrowAnyException();
        thread2.start();
        assertThatCode(() -> {
            thread1.join();
            thread2.join();
        }).doesNotThrowAnyException();
    }

    /**
     * In this test, we spend 5 NOK twice in close succession on a campaign. This should make the campaign unavailable
     * for further spending.
     * After this, we will wait for 9 seconds, and the campaign should still be unavailable.
     * After another 1 seconds and 1 millisecond of waiting, the campaign should become available once again,
     * since 1.0001+9 > 10 so the first 2 spendings will be too old to be included in the spendings of the past 10 seconds.
     */
    @Test
    void spending(){
        long campaignId = 1;
        assertThatCode(() -> {
            bidSynchronizer.lockCampaign(campaignId);
            assertThat(bidSynchronizer.isCampaignAvailableForSpending(campaignId, 5)).isTrue();
            bidSynchronizer.spendOnCampaign(campaignId, 5);
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException();
        assertThatCode(() -> {
            bidSynchronizer.lockCampaign(campaignId);
            assertThat(bidSynchronizer.isCampaignAvailableForSpending(campaignId, 5)).isTrue();
            bidSynchronizer.spendOnCampaign(campaignId, 5);
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException();
        assertThatCode(() -> {
            bidSynchronizer.lockCampaign(campaignId);
            assertThat(bidSynchronizer.isCampaignAvailableForSpending(campaignId, 5)).isFalse();
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException();
        assertThatCode(() -> Thread.sleep(9000)).doesNotThrowAnyException();
        assertThatCode(() -> {
            bidSynchronizer.lockCampaign(campaignId);
            assertThat(bidSynchronizer.isCampaignAvailableForSpending(campaignId, 5)).isFalse();
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException();
        assertThatCode(() -> Thread.sleep(1001)).doesNotThrowAnyException();
        assertThatCode(() -> {
            bidSynchronizer.lockCampaign(campaignId);
            assertThat(bidSynchronizer.isCampaignAvailableForSpending(campaignId, 5)).isTrue();
            bidSynchronizer.spendOnCampaign(campaignId, 5);
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException();
    }

    @Test
    void accessWithoutLockException(){
        long campaignId = 1;
        assertThatExceptionOfType(IllegalThreadStateException.class).isThrownBy(() -> bidSynchronizer.isCampaignAvailableForSpending(campaignId, 10));
        assertThatExceptionOfType(IllegalThreadStateException.class).isThrownBy(() -> bidSynchronizer.spendOnCampaign(campaignId, 10));

        Thread thread1 = new Thread(() -> assertThatCode(() -> {
            bidSynchronizer.lockCampaign(campaignId);
            Thread.sleep(500);
            bidSynchronizer.unlockCampaign(campaignId);
        }).doesNotThrowAnyException());
        Thread thread2 = new Thread(() -> {
            assertThatExceptionOfType(IllegalThreadStateException.class).isThrownBy(() -> bidSynchronizer.isCampaignAvailableForSpending(campaignId, 10));
            assertThatExceptionOfType(IllegalThreadStateException.class).isThrownBy(() -> bidSynchronizer.spendOnCampaign(campaignId, 10));
        });
        thread1.start();
        assertThatCode(() -> Thread.sleep(50)).doesNotThrowAnyException();
        thread2.start();
        assertThatCode(() -> {
            thread1.join();
            thread2.join();
        }).doesNotThrowAnyException();
    }
}
