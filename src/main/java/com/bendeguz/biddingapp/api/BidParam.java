package com.bendeguz.biddingapp.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

public class BidParam {
    @NotNull
    private Long id;

    @NotNull
    private String[] keywords;

    public BidParam() {
        // Jackson deserialization
    }

    public BidParam(long id, String[] keywords) {
        this.id = id;
        this.keywords = keywords;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public String[] getKeywords(){
        return keywords;
    }

    @Override
    public String toString() {
        return "BidParam{" + "id=" + id + ", keywords=" + Arrays.toString(keywords) + '}';
    }
}
