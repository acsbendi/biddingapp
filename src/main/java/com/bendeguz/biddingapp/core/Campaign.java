package com.bendeguz.biddingapp.core;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "campaigns")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.bendeguz.biddingapp.core.Campaign.findAll",
                        query = "SELECT c FROM Campaign c"
                )
        })
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false)
    @NotNull
    private String name;

    /**
     *  Perhaps it's not the most obvious solution to use a Set but everything else failed to work:
     *
     *  - When I tried to map the field to h2's built-in array type, the following exception occurred:
     *      org.h2.jdbc.JdbcSQLException: Hexadecimal string contains non-hex character
     *    As I could not find any sources online regarding this mapping, I concluded this is not possible.
     *
     *  - When I tried to use Array[] with ElementCollection and CollectionTable, the following exception occurred:
     *      org.hibernate.AnnotationException: List/array has to be annotated with an @OrderColumn
     *    As far as I could find out, this means that I should create another column just for the ordering.
     *    I think this solution would have been more hacky, so I went with using a Set type instead.
     *
     *  I also think that it makes sense to use a set, as a duplicate keyword wouldn't add any value anyway.
     *  This is why I decided to use a Set instead of a List.
     *
     */
    @ElementCollection
    @CollectionTable(
            name = "keywords",
            joinColumns = @JoinColumn(name = "campaign_id")
    )
    @Column(name = "keyword", nullable = false)
    @NotNull
    private Set<String> keywords = new HashSet<>();

    @Column(name = "budget", nullable = false)
    private double budget;

    @Column(name = "spending", nullable = false)
    private double spending;

    public Campaign() {
        // Jackson deserialization
    }

    public Campaign(String name, String[] keywords, double budget) {
        this.name = name;
        this.keywords.addAll(Arrays.asList(keywords));
        this.budget = budget;
        this.spending = 0;
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

    public Set<String> getKeywords(){
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public double getBudget(){
        return budget;
    }

    public void setBudget(long budget) {
        this.budget = budget;
    }

    public double getSpending(){
        return spending;
    }

    public void setSpending(long spending) {
        this.spending = spending;
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
                Arrays.equals(this.keywords.toArray(), that.keywords.toArray());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, budget, Arrays.hashCode(keywords.toArray()));
    }
}
