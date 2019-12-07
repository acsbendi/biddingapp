package com.bendeguz.biddingapp;

import com.bendeguz.biddingapp.core.Campaign;
import com.bendeguz.biddingapp.db.CampaignDAO;
import com.bendeguz.biddingapp.resources.BidsResource;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import com.bendeguz.biddingapp.resources.CampaignsResource;

public class BiddingApplication extends Application<BiddingConfiguration> {
    public static void main(String[] args) throws Exception {
        new BiddingApplication().run(args);
    }

    private final HibernateBundle<BiddingConfiguration> hibernateBundle =
            new HibernateBundle<BiddingConfiguration>(Campaign.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(BiddingConfiguration configuration) {
                    return configuration.getDataSourceFactory();
                }
            };

    @Override
    public String getName() {
        return "bidding-app";
    }

    @Override
    public void initialize(Bootstrap<BiddingConfiguration> bootstrap) {
        bootstrap.addBundle(new MigrationsBundle<BiddingConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(BiddingConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(hibernateBundle);
    }

    @Override
    public void run(BiddingConfiguration configuration,
                    Environment environment) {
        final CampaignDAO campaignDAO = new CampaignDAO(hibernateBundle.getSessionFactory());

        final CampaignsResource campaignsResource = new CampaignsResource(campaignDAO);
        final BidsResource bidsResource = new BidsResource(campaignDAO);
        environment.jersey().register(campaignsResource);
        environment.jersey().register(bidsResource);
    }

}