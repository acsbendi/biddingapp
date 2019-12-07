package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.api.CampaignParam;
import com.bendeguz.biddingapp.core.Campaign;
import com.codahale.metrics.annotation.Timed;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.atomic.AtomicLong;

@Path("/campaigns")
@Produces(MediaType.APPLICATION_JSON)
public class CampaignsResource {
    private final AtomicLong counter;

    public CampaignsResource() {
        this.counter = new AtomicLong();
    }

    @POST
    @Consumes("application/json")
    @Timed
    public Campaign createCampaign(@NotNull CampaignParam campaignParam) {
        return new Campaign(counter.incrementAndGet(), campaignParam.getName(), campaignParam.getKeywords(), campaignParam.getBudget());
    }
}