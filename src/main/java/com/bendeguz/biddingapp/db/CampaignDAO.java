package com.bendeguz.biddingapp.db;

import com.bendeguz.biddingapp.core.Campaign;
import io.dropwizard.hibernate.AbstractDAO;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class CampaignDAO extends AbstractDAO<Campaign> {
    public CampaignDAO(SessionFactory factory) {
        super(factory);
    }

    public Optional<Campaign> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public Campaign create(Campaign campaign) {
        return persist(campaign);
    }

    @SuppressWarnings("unchecked")
    public List<Campaign> findAll() {
        return list((Query<Campaign>) namedQuery(Campaign.QUERY_FIND_ALL));
    }

    /**
     * Finds campaigns that have at least one of the specified keywords and that have a positive balance
     * (budget - spending) as well. This is done by using a named query for performance reasons -
     * retrieving all campaigns using findAll would have a potentially large negative effect on performance.
     *
     * @param keywords The keywords that the returned campaigns should contain.
     * @return a list of the campaigns matching the above criteria.
     */
    @SuppressWarnings("unchecked")
    public List<Campaign> findCampaignsWithPositiveBalanceByKeywords(String[] keywords) {
        Query query = namedQuery(Campaign.QUERY_FIND_CAMPAIGNS_WITH_POSITIVE_BALANCE_BY_KEYWORDS);
        query.setParameterList("keywords", keywords);
        return list(query);
    }

    /**
     * Tries to increase the spending for a {@link Campaign}.
     *
     * @param campaign The campaign to increase spending for.
     * @param amount The amount by which to increase the spending.
     * @return a success flag, {@code true} if the increase is successful, {@code false} otherwise.
     */
    public boolean tryToIncreaseSpending(Campaign campaign, double amount) {
        Query query = namedQuery(Campaign.QUERY_INCREASE_SPENDING);
        query.setParameter("id", campaign.getId());
        query.setParameter("increase", amount);
        return query.executeUpdate() > 0;
    }
}
