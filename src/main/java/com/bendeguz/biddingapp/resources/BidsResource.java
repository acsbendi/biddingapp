package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.BiddingConfiguration;
import com.bendeguz.biddingapp.api.BidParam;
import com.bendeguz.biddingapp.api.BidResult;
import com.bendeguz.biddingapp.core.Campaign;
import com.bendeguz.biddingapp.db.CampaignDAO;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.*;

@Path("/bids")
@Produces(MediaType.APPLICATION_JSON)
public class BidsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(BidsResource.class);
    private static final double BID_AMOUNT = 1.0;
    private static final int BID_TIMEOUT_IN_MILLISECONDS = 500;

    /**
     * This static nested class is used to decouple the execution of bidding so it can ensured that it will always
     * take less than BID_TIMEOUT_IN_MILLISECONDS.
     *
     * It needs to be implemented in a separate class so we can use the @UnitOfWork annotation to access the database
     * without having to worry about session management.
     *
     * Also, a static class is needed because its instantiation happens outside of BidsResource
     * (in the UnitOfWorkAwareProxyFactory.created method).
     */
    private static class TryToBidCallable implements Callable<Boolean> {
        private final Random random = new Random();
        private final CampaignDAO campaignDAO;
        private final String[] keywords;

        TryToBidCallable(CampaignDAO campaignDAO, String[] keywords){
            this.campaignDAO = campaignDAO;
            this.keywords = keywords;
        }

        @Override
        @UnitOfWork
        public Boolean call() {
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

    private final CampaignDAO campaignDAO;
    private final ExecutorService executorService;
    /**
     * This field is used to create TryToBidCallable instances through the UnitOfWorkAwareProxyFactory so
     * the @UnitOfWork annotation can be added to these instances.
     */
    private final HibernateBundle<BiddingConfiguration> hibernateBundle;

    public BidsResource(CampaignDAO campaignDAO, ExecutorService executorService, HibernateBundle<BiddingConfiguration> hibernateBundle) {
        this.campaignDAO = campaignDAO;
        this.executorService = executorService;
        this.hibernateBundle = hibernateBundle;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBid(@NotNull @Valid BidParam bidParam) {
        if(tryToBid(bidParam.getKeywords())){
            BidResult result = new BidResult(bidParam.getBidId(), BID_AMOUNT);
            return Response.ok(result).build();
        } else {
            return Response.noContent().build();
        }
    }

    /**
     * Tries to place a bid by searching a campaign that contains one of the specified keywords.
     * This function is guaranteed to return after 500 milliseconds, if there is something preventing the bid from
     * succeeding in this time frame, the bidding attempt is cancelled and false is returned.
     *
     * @param keywords The keywords to search in the campaigns to bid for.
     * @return Success flag, true if the bid is successful, false otherwise.
     */
    private boolean tryToBid(String[] keywords){
        // This creation mechanism ensures that the @UnitOfWork annotation can be added to methods of the created
        // TryToBidCallable instance.
        UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory = new UnitOfWorkAwareProxyFactory(hibernateBundle);
        TryToBidCallable tryToBidCallable = unitOfWorkAwareProxyFactory.create(
                TryToBidCallable.class,
                new Class[]{CampaignDAO.class, String[].class},
                new Object[]{campaignDAO, keywords}
        );
        Future<Boolean> tryToBidFuture = executorService.submit(tryToBidCallable);

        try {
            return tryToBidFuture.get(BID_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Exception occurred while trying to place a bid: ", e);
            return false;
        } catch (TimeoutException e) {
            LOGGER.info("Cancelling bid due to timeout");
            tryToBidFuture.cancel(true);
            return false;
        }
    }
}
