package com.bendeguz.biddingapp.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class Campaign {
    private long id;

    private String name;

    private String[] keywords;

    private double budget;

    public Campaign() {
        // Jackson deserialization
    }

    public Campaign(long id, String name, String[] keywords, double budget) {
        this.id = id;
        this.name = name;
        this.keywords = keywords;
        this.budget = budget;
    }

    @JsonProperty
    public long getId() {
        return id;
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
        return "Campaign{" + "id=" + id + ", name=" + name + ", keywords=" + Arrays.toString(keywords) + ", budget=" + budget + '}';
    }
}
