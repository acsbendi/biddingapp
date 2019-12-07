package com.bendeguz.biddingapp.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

public class BidParam {
    @NotNull
    private Long bidId;

    @NotNull
    private String[] keywords;

    public BidParam() {
        // Jackson deserialization
    }

    public BidParam(long bidId, String[] keywords) {
        this.bidId = bidId;
        this.keywords = keywords;
    }

    @JsonProperty
    public long getBidId() {
        return bidId;
    }

    @JsonProperty
    public String[] getKeywords(){
        return keywords;
    }

    @Override
    public String toString() {
        return "BidParam{" + "bidId=" + bidId + ", keywords=" + Arrays.toString(keywords) + '}';
    }
}
