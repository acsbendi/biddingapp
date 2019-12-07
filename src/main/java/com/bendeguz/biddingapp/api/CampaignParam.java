package com.bendeguz.biddingapp.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

public class CampaignParam {
    @NotNull
    private String name;

    @NotNull
    private String[] keywords;

    @NotNull
    private Double budget;

    public CampaignParam() {
        // Jackson deserialization
    }

    public CampaignParam(String name, String[] keywords, Double budget) {
        this.name = name;
        this.keywords = keywords;
        this.budget = budget;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String[] getKeywords(){
        return keywords;
    }

    @JsonProperty
    public double getBudget(){
        return budget;
    }

    @Override
    public String toString() {
        return "CampaignParam{" + "name=" + name + ", keywords=" + Arrays.toString(keywords) + ", budget=" + budget + '}';
    }
}
