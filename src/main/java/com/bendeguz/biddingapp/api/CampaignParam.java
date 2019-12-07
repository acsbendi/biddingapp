package com.bendeguz.biddingapp.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class CampaignParam {
    private String name;

    private String[] keywords;

    private double budget;

    public CampaignParam() {
        // Jackson deserialization
    }

    public CampaignParam(String name, String[] keywords, double budget) {
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
