package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.BidSynchronizer;
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
     * take less than {@code BID_TIMEOUT_IN_MILLISECONDS}.
     * <p>
     * It needs to be implemented in a separate class so we can use the {@code @UnitOfWork} annotation to access the database
     * without having to worry about session management.
     * <p>
     * Also, a static class is needed because its instantiation happens outside of BidsResource
     * (in the {@code UnitOfWorkAwareProxyFactory.create} method).
     */
    private static class TryToBidCallable implements Callable<Boolean> {
        private final Random random = new Random();
        private final CampaignDAO campaignDAO;
        private final String[] keywords;
        private final BidSynchronizer bidSynchronizer;

        TryToBidCallable(CampaignDAO campaignDAO, String[] keywords, BidSynchronizer bidSynchronizer) {
            this.campaignDAO = campaignDAO;
            this.keywords = keywords;
            this.bidSynchronizer = bidSynchronizer;
        }

        /**
         * Tries to bid on the specified campaign. Locks it, then proceeds only if it is available for spending.
         * If an exception is thrown the bid can be considered unsuccessful.
         *
         * @param campaign The campaign to bid on.
         * @return a success flag, {@code true} if the bid is successful, {@code false} otherwise.
         * @throws InterruptedException if the thread gets interrupted.
         */
        private boolean tryToBidOnCampaign(Campaign campaign) throws InterruptedException {
            try {
                bidSynchronizer.lockCampaign(campaign.getId());
                if (bidSynchronizer.isCampaignAvailableForSpending(campaign.getId(), BID_AMOUNT)) {
                    // Check if thread has been interrupted - proceed only if not.
                    // This helps ensure that the bidding never takes longer than BID_TIMEOUT_IN_MILLISECONDS.
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    // Saving the fact of spending first - this helps ensure that no excessive spending is ever carried out.
                    // Note that the spending might not actually happen (if the save method fails for example due to
                    // an interrupt event), but this is not a serious problem, since the caller will still see it as failure.
                    bidSynchronizer.spendOnCampaign(campaign.getId(), BID_AMOUNT);
                    return campaignDAO.tryToIncreaseSpending(campaign, BID_AMOUNT);
                }
            } finally {
                bidSynchronizer.unlockCampaign(campaign.getId());
            }
            return false;
        }

        @Override
        @UnitOfWork
        public Boolean call() throws Exception {
            List<Campaign> campaigns = campaignDAO.findCampaignsWithPositiveBalanceByKeywords(keywords);
            while (!campaigns.isEmpty()) {
                Campaign campaign = campaigns.get(random.nextInt(campaigns.size()));
                campaigns.remove(campaign);
                if (tryToBidOnCampaign(campaign)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final CampaignDAO campaignDAO;
    private final ExecutorService executorService;
    private final BidSynchronizer bidSynchronizer;
    /**
     * This field is used to create {@code TryToBidCallable} instances through the {@code UnitOfWorkAwareProxyFactory}
     * so the {@code @UnitOfWork} annotation can be added to these instances.
     */
    private final HibernateBundle<BiddingConfiguration> hibernateBundle;

    public BidsResource(CampaignDAO campaignDAO, ExecutorService executorService,
                        HibernateBundle<BiddingConfiguration> hibernateBundle, BidSynchronizer bidSynchronizer) {
        this.campaignDAO = campaignDAO;
        this.executorService = executorService;
        this.hibernateBundle = hibernateBundle;
        this.bidSynchronizer = bidSynchronizer;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBid(@NotNull @Valid BidParam bidParam) {
        if (tryToBid(bidParam.getKeywords())) {
            BidResult result = new BidResult(bidParam.getBidId(), BID_AMOUNT);
            return Response.ok(result).build();
        } else {
            return Response.noContent().build();
        }
    }

    /**
     * Tries to place a bid by searching a campaign that contains one of the specified keywords.
     * This function is guaranteed to return after {@code BID_TIMEOUT_IN_MILLISECONDS} milliseconds, if there is
     * something preventing the bid from succeeding in this time frame, the bidding attempt is cancelled and
     * {@code false} is returned.
     *
     * @param keywords The keywords to search in the campaigns to bid for.
     * @return a success flag, {@code true} if the bid is successful, {@code false} otherwise.
     */
    private boolean tryToBid(String[] keywords) {
        // This creation mechanism ensures that the @UnitOfWork annotation can be added to methods of the created
        // TryToBidCallable instance.
        UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory = new UnitOfWorkAwareProxyFactory(hibernateBundle);
        TryToBidCallable tryToBidCallable = unitOfWorkAwareProxyFactory.create(
                TryToBidCallable.class,
                new Class[]{CampaignDAO.class, String[].class, BidSynchronizer.class},
                new Object[]{campaignDAO, keywords, bidSynchronizer}
        );
        Future<Boolean> tryToBidFuture = executorService.submit(tryToBidCallable);

        return getSuccessFlagFromTryToBidFuture(tryToBidFuture);
    }

    /**
     * Determines and returns the success flag from the {@link Future} object which represents the execution of a bid.
     * This method will interrupt the future after {@code BID_TIMEOUT_IN_MILLISECONDS} milliseconds.
     * If the interrupt fails, the result of the future is awaited without further timeout - this is to ensure that
     * the client will always see the right status code.
     *
     * @param tryToBidFuture A {@link Future} object representing the execution of a bid.
     * @return whether the bid was successful.
     */
    private boolean getSuccessFlagFromTryToBidFuture(Future<Boolean> tryToBidFuture) {
        try {
            return tryToBidFuture.get(BID_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Exception occurred while trying to place a bid: ", e);
            return false;
        } catch (TimeoutException e) {
            LOGGER.info("Cancelling bid due to timeout");
            if(tryToBidFuture.cancel(true)){
                return false;
            } else { // the bidding job should be done
                LOGGER.warn("The bidding job could not be cancelled - waiting for a result...");
                try {
                    return tryToBidFuture.get();
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.warn("Exception occurred while trying to place a bid: ", ex);
                    return false;
                }
            }
        }
    }
}
