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

    public void save(Campaign campaign) {
        persist(campaign);
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
}
