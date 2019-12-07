package com.bendeguz.biddingapp.core;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "campaigns")
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false)
    @NotNull
    private String name;

    @Column(name = "keywords", nullable = false)
    @NotNull
    private String[] keywords;

    @Column(name = "budget", nullable = false)
    private double budget;

    public Campaign() {
        // Jackson deserialization
    }

    public Campaign(String name, String[] keywords, double budget) {
        this.name = name;
        this.keywords = keywords;
        this.budget = budget;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getKeywords(){
        return keywords;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public double getBudget(){
        return budget;
    }

    public void setBudget(long budget) {
        this.budget = budget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Campaign)) {
            return false;
        }

        final Campaign that = (Campaign) o;

        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.budget, that.budget) &&
                Arrays.equals(this.keywords, that.keywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, budget, Arrays.hashCode(keywords));
    }
}
