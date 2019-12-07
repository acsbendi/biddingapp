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
        return list((Query<Campaign>) namedQuery("com.bendeguz.biddingapp.core.Campaign.findAll"));
    }
}
