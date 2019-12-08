package com.bendeguz.biddingapp.core;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

@Entity
@Table(name = "campaigns")
@NamedQueries(
        {
                @NamedQuery(
                        name = Campaign.QUERY_FIND_ALL,
                        query = "SELECT c FROM Campaign c"
                ),
                @NamedQuery(
                        name = Campaign.QUERY_FIND_CAMPAIGNS_WITH_POSITIVE_BALANCE_BY_KEYWORDS,
                        query = "SELECT c FROM Campaign AS c JOIN c.keywords AS keyword WHERE keyword IN (:keywords) AND c.budget - c.spending > 0"
                ),
                @NamedQuery(
                        name = Campaign.QUERY_INCREASE_SPENDING,
                        query = "UPDATE Campaign SET spending = spending + :increase WHERE id = :id AND budget - spending - :increase >= 0"
                )
        })
public class Campaign {
    public static final String QUERY_FIND_ALL = "com.bendeguz.biddingapp.core.Campaign.findAll";
    /**
     * Query to increase a campaign's spending by the specified amount. Succeeds only if the campaign's balance
     * (budget - spending) remains 0 or greater after the spending increase. Returns the number of updates
     * (i.e. 1 if it succeeds, 0 if it doesn't).
     */
    public static final String QUERY_INCREASE_SPENDING = "com.bendeguz.biddingapp.core.Campaign.increaseSpending";
    /**
     * Query to find the campaigns that have at least one of the specified keywords, and a positive balance.
     * The balance of a campaign is defined as the difference between its budget and its spending.
     * <p>
     * The implementation of this query is a bit unusual, the campaigns are joined by one of its own fields.
     * The reason why it has to be done this way is that there's no INTERSECT operator in HQL.
     */
    public static final String QUERY_FIND_CAMPAIGNS_WITH_POSITIVE_BALANCE_BY_KEYWORDS = "com.bendeguz.biddingapp.core.Campaign.findCampaignsWithPositiveBalanceByKeywords";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false)
    @NotNull
    private String name;

    /**
     * Perhaps it's not the most obvious solution to use a Set but everything else failed to work:
     * - When I tried to map the field to h2's built-in array type, the following exception occurred:
     *   {@code org.h2.jdbc.JdbcSQLException}: Hexadecimal string contains non-hex character
     *   As I could not find any sources online regarding this mapping, I concluded this is not possible.
     * - When I tried to use {@code Array[]} with {@code @ElementCollection} and {@code @CollectionTable}, the following exception occurred:
     *   {@code org.hibernate.AnnotationException}: List/array has to be annotated with an {@code @OrderColumn}
     *   As far as I could find out, this means that I should create another column just for the ordering.
     *   I think this solution would have been more hacky, so I went with using a Set type instead.
     * <p>
     * I also think that it makes sense to use a set, as a duplicate keyword wouldn't add any value anyway.
     * This is why I decided to use a {@link Set} instead of a {@link List}.
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

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(long budget) {
        this.budget = budget;
    }

    public double getSpending() {
        return spending;
    }

    public void setSpending(double spending) {
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
