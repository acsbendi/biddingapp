package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.api.BidParam;
import com.bendeguz.biddingapp.api.BidResult;
import com.bendeguz.biddingapp.core.Campaign;
import com.bendeguz.biddingapp.db.CampaignDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Random;

@Path("/bids")
@Produces(MediaType.APPLICATION_JSON)
public class BidsResource {
    private static final double BID_AMOUNT = 1.0;

    private final CampaignDAO campaignDAO;
    private final Random random = new Random();

    public BidsResource(CampaignDAO campaignDAO) {
        this.campaignDAO = campaignDAO;
    }

    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBid(@NotNull @Valid BidParam bidParam) {
        if(tryToBid(bidParam.getKeywords())){
            BidResult result = new BidResult(bidParam.getId(), BID_AMOUNT);
            return Response.ok(result).build();
        } else {
            return Response.noContent().build();
        }
    }

    private boolean tryToBid(String[] keywords){
        List<Campaign> campaigns = campaignDAO.findCampaignsWithPositiveBalanceByKeywords(keywords);
        if(!campaigns.isEmpty()) {
            Campaign campaign = campaigns.get(random.nextInt(campaigns.size()));
            double currentSpending = campaign.getSpending();
            campaign.setSpending(currentSpending + BID_AMOUNT);
            campaignDAO.save(campaign);
            return true;
        }
        return false;
    }
}
