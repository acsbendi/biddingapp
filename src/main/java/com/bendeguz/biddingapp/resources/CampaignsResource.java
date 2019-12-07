package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.api.CampaignParam;
import com.bendeguz.biddingapp.core.Campaign;
import com.bendeguz.biddingapp.db.CampaignDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

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
    public Response createCampaign(@NotNull CampaignParam campaignParam) {
        Campaign campaign = new Campaign(campaignParam.getName(), campaignParam.getKeywords(), campaignParam.getBudget());
        Campaign createdCampaign = campaignDAO.create(campaign);

        URI createdUri = URI.create("/campaigns/" + createdCampaign.getId());
        return Response.created(createdUri).build();
    }

    @GET
    @UnitOfWork
    public List<Campaign> getCampaigns() {
        return campaignDAO.findAll();
    }

    @GET
    @Path("/{campaignId}")
    @UnitOfWork
    public Campaign getCampaignById(@NotNull @PathParam("campaignId") long campaignId) {
        return campaignDAO.findById(campaignId).orElseThrow(() -> new NotFoundException("No such campaign."));
    }
}