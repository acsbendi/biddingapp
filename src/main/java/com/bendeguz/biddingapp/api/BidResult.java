package com.bendeguz.biddingapp.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BidResult {
    private long bidId;

    private double bidAmount;

    public BidResult() {
        // Jackson deserialization
    }

    public BidResult(long bidId, double bidAmount) {
        this.bidId = bidId;
        this.bidAmount = bidAmount;
    }

    @JsonProperty
    public long getBidId() {
        return bidId;
    }

    @JsonProperty
    public double getBidAmount() {
        return bidAmount;
    }

    @Override
    public String toString() {
        return "BidResult{" + "bidId=" + bidId + ", bidAmount=" + bidAmount + '}';
    }
}
