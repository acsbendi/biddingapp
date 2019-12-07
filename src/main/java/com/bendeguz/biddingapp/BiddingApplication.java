package com.bendeguz.biddingapp;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import com.bendeguz.biddingapp.resources.CampaignsResource;

public class BiddingApplication extends Application<BiddingConfiguration> {
    public static void main(String[] args) throws Exception {
        new BiddingApplication().run(args);
    }

    @Override
    public String getName() {
        return "bidding-app";
    }

    @Override
    public void initialize(Bootstrap<BiddingConfiguration> bootstrap) {
        // nothing to do yet
    }

    @Override
    public void run(BiddingConfiguration configuration,
                    Environment environment) {
        final CampaignsResource resource = new CampaignsResource();
        environment.jersey().register(resource);
    }

}