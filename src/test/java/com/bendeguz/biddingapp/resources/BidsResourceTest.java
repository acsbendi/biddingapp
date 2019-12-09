package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.BidSynchronizer;
import com.bendeguz.biddingapp.api.BidParam;
import com.bendeguz.biddingapp.api.BidResult;
import com.bendeguz.biddingapp.core.Campaign;
import com.bendeguz.biddingapp.db.CampaignDAO;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BidsResource}.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class BidsResourceTest {
    /**
     * This class is used to make {@link HibernateBundle}'s {@code name()} method public.
     * Otherwise, we cannot mock the {@code name()} method and the tests will fail with a {@code NullPointerException}.
     * Inspiration: https://stackoverflow.com/a/46203712/3738870.
     */
    static abstract class HibernateBundleMock extends HibernateBundle{
        protected HibernateBundleMock(Class entity, Class[] entities) {
            super(entity, entities);
        }

        @Override
        public String name(){
            return "hibernate";
        }
    }

    private static final CampaignDAO CAMPAIGN_DAO = mock(CampaignDAO.class);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static final HibernateBundleMock HIBERNATE_BUNDLE = mock(HibernateBundleMock.class);
    private static final SessionFactory SESSION_FACTORY = mock(SessionFactory.class);
    private static final Session SESSION = mock(Session.class);
    private static final BidSynchronizer BID_SYNCHRONIZER = new BidSynchronizer();
    private static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .addResource(new BidsResource(CAMPAIGN_DAO, EXECUTOR_SERVICE, HIBERNATE_BUNDLE, BID_SYNCHRONIZER))
            .build();

    private final ArgumentCaptor<String[]> keywordArrayCaptor = ArgumentCaptor.forClass(String[].class);
    private final ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        campaign = new Campaign();
        campaign.setName("Test Campaign");
        campaign.setKeywords(new HashSet<>(Collections.singletonList("Keyword 1")));
        campaign.setBudget(100);
    }

    @AfterEach
    void tearDown() {
        reset(CAMPAIGN_DAO);
    }

    @Test
    void createBid(){
        when(CAMPAIGN_DAO.findCampaignsWithPositiveBalanceByKeywords(any(String[].class))).thenReturn(
                new ArrayList<>(Collections.singletonList(campaign))
        );
        when(CAMPAIGN_DAO.tryToIncreaseSpending(any(Campaign.class), any(Double.class))).thenReturn(true);
        // The following mocks needs to be set up so we will not run into NullPointerException when various methods of
        // HIBERNATE_BUNDLE are called.
        when(HIBERNATE_BUNDLE.getSessionFactory()).thenReturn(SESSION_FACTORY);
        when(HIBERNATE_BUNDLE.name()).thenReturn("test");
        when(SESSION_FACTORY.openSession()).thenReturn(SESSION);

        BidParam bidParam = new BidParam(1, new String[]{"Keyword 1"});
        final Response response = RESOURCES.target("/bids")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(bidParam, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        BidResult bidResult = response.readEntity(BidResult.class);
        assertThat(bidResult.getBidAmount()).isEqualTo(1);
        assertThat(bidResult.getBidId()).isEqualTo(1);
        verify(CAMPAIGN_DAO).findCampaignsWithPositiveBalanceByKeywords(keywordArrayCaptor.capture());
        assertThat(keywordArrayCaptor.getValue()).containsOnly("Keyword 1");
        verify(CAMPAIGN_DAO).tryToIncreaseSpending(campaignCaptor.capture(), any(Double.class));
        assertThat(campaignCaptor.getValue()).isEqualTo(campaign);
    }

    @Test
    void createBidUnsuccessfulUpdate(){
        when(CAMPAIGN_DAO.findCampaignsWithPositiveBalanceByKeywords(any(String[].class))).thenReturn(
                new ArrayList<>(Collections.singletonList(campaign))
        );
        when(CAMPAIGN_DAO.tryToIncreaseSpending(any(Campaign.class), any(Double.class))).thenReturn(false);
        // The following mocks needs to be set up so we will not run into NullPointerException when various methods of
        // HIBERNATE_BUNDLE are called.
        when(HIBERNATE_BUNDLE.getSessionFactory()).thenReturn(SESSION_FACTORY);
        when(HIBERNATE_BUNDLE.name()).thenReturn("test");
        when(SESSION_FACTORY.openSession()).thenReturn(SESSION);

        BidParam bidParam = new BidParam(1, new String[]{"Keyword 1"});
        final Response response = RESOURCES.target("/bids")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(bidParam, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
        verify(CAMPAIGN_DAO).findCampaignsWithPositiveBalanceByKeywords(keywordArrayCaptor.capture());
        assertThat(keywordArrayCaptor.getValue()).containsOnly("Keyword 1");
        verify(CAMPAIGN_DAO).tryToIncreaseSpending(campaignCaptor.capture(), any(Double.class));
        assertThat(campaignCaptor.getValue()).isEqualTo(campaign);
    }

    @Test
    void createBidUnsuccessfulNoMatchingKeywords(){
        when(CAMPAIGN_DAO.findCampaignsWithPositiveBalanceByKeywords(any(String[].class))).thenReturn(new ArrayList<>());
        // The following mocks needs to be set up so we will not run into NullPointerException when various methods of
        // HIBERNATE_BUNDLE are called.
        when(HIBERNATE_BUNDLE.getSessionFactory()).thenReturn(SESSION_FACTORY);
        when(HIBERNATE_BUNDLE.name()).thenReturn("test");
        when(SESSION_FACTORY.openSession()).thenReturn(SESSION);

        BidParam bidParam = new BidParam(1, new String[]{"Keyword 2"});
        final Response response = RESOURCES.target("/bids")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(bidParam, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
        verify(CAMPAIGN_DAO).findCampaignsWithPositiveBalanceByKeywords(keywordArrayCaptor.capture());
        assertThat(keywordArrayCaptor.getValue()).containsOnly("Keyword 2");
    }
}
