package com.bendeguz.biddingapp.db;

import com.bendeguz.biddingapp.core.Campaign;
import io.dropwizard.hibernate.AbstractDAO;

import org.hibernate.SessionFactory;

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
}
