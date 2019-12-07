package com.bendeguz.biddingapp.db;

import com.bendeguz.biddingapp.core.Campaign;
import io.dropwizard.hibernate.AbstractDAO;

import org.hibernate.SessionFactory;

public class CampaignDAO extends AbstractDAO<Campaign> {
    public CampaignDAO(SessionFactory factory) {
        super(factory);
    }

    public Campaign create(Campaign campaign) {
        return persist(campaign);
    }
}
