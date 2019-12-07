package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.api.CampaignParam;
import com.bendeguz.biddingapp.core.Campaign;
import com.bendeguz.biddingapp.db.CampaignDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/campaigns")
@Produces(MediaType.APPLICATION_JSON)
public class CampaignsResource {
    private final CampaignDAO campaignDAO;

    public CampaignsResource(CampaignDAO campaignDAO) {
        this.campaignDAO = campaignDAO;
    }

    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    public Campaign createCampaign(@NotNull CampaignParam campaignParam) {
        Campaign campaign = new Campaign(campaignParam.getName(), campaignParam.getKeywords(), campaignParam.getBudget());
        return this.campaignDAO.create(campaign);
    }
}