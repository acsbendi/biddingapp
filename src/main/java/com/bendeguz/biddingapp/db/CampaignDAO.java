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

    @SuppressWarnings("unchecked")
    public List<Campaign> findCampaignsWithPositiveBalanceByKeywords(String[] keywords) {
        Query query = namedQuery(Campaign.QUERY_FIND_CAMPAIGNS_WITH_POSITIVE_BALANCE_BY_KEYWORDS);
        query.setParameterList("keywords", keywords);
        return list(query);
    }

    /**
     * Tries to increase the spending for a Campaign.
     *
     * @param campaign The campaign to increase spending for.
     * @param amount The amount by which to increase the spending.
     * @return Success flag, true if the increase is successful, false otherwise.
     */
    public boolean tryToIncreaseSpending(Campaign campaign, double amount) {
        Query query = namedQuery(Campaign.QUERY_INCREASE_SPENDING);
        query.setParameter("id", campaign.getId());
        query.setParameter("increase", amount);
        return query.executeUpdate() > 0;
    }
}
