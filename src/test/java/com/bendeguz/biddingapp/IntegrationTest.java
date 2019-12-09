package com.bendeguz.biddingapp;

import com.bendeguz.biddingapp.api.BidParam;
import com.bendeguz.biddingapp.api.BidResult;
import com.bendeguz.biddingapp.api.CampaignParam;
import com.bendeguz.biddingapp.core.Campaign;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
class IntegrationTest {

    private static final String TMP_FILE = createTempFile();
    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-biddingapp.yml");

    private static final DropwizardAppExtension<BiddingConfiguration> RULE = new DropwizardAppExtension<>(
            BiddingApplication.class, CONFIG_PATH,
            ConfigOverride.config("database.url", "jdbc:h2:" + TMP_FILE));

    @BeforeAll
    static void migrateDb() throws Exception {
        RULE.getApplication().run("db", "migrate", CONFIG_PATH);
    }

    private static String createTempFile() {
        try {
            return File.createTempFile("test-example", null).getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void testPostAndGetCampaign() {
        final CampaignParam campaignParam = new CampaignParam("Test Campaign 1", new String[]{"Test Keyword"}, 100.0);
        final Campaign createdCampaign = postAndGetCampaign(campaignParam);

        assertThat(createdCampaign.getId()).isNotNull();
        assertThat(createdCampaign.getName()).isEqualTo(campaignParam.getName());
        assertThat(createdCampaign.getKeywords()).containsOnly(campaignParam.getKeywords()[0]);
        assertThat(createdCampaign.getBudget()).isEqualTo(campaignParam.getBudget());
        assertThat(createdCampaign.getSpending()).isEqualTo(0.0);
    }

    private Campaign postAndGetCampaign(CampaignParam campaignParam) {
        String location = RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/campaigns")
                .request()
                .post(Entity.entity(campaignParam, MediaType.APPLICATION_JSON_TYPE))
                .getHeaderString("Location");
        return RULE.client().target(location)
                .request()
                .get()
                .readEntity(Campaign.class);
    }

    @Test
    void testListCampaigns(){
        CampaignParam campaignParam1 = new CampaignParam("Test Campaign 11", new String[]{"Kobler"}, 150.0);
        CampaignParam campaignParam2 = new CampaignParam("Test Campaign 12", new String[]{"Test Keyword 10", "Kobler 2", "Test 2"}, 2500.0);
        createCampaign(campaignParam1);
        createCampaign(campaignParam2);

        Response response = RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/campaigns")
                .request()
                .get();

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        Campaign[] returnedCampaigns = response.readEntity(Campaign[].class);
        assertThat(returnedCampaigns).extracting("name").contains("Test Campaign 11", "Test Campaign 12");
        assertThat(returnedCampaigns).flatExtracting("keywords").contains("Kobler", "Test Keyword 10", "Kobler 2", "Test 2");
        assertThat(returnedCampaigns).extracting("budget").contains(150.0, 2500.0);
        assertThat(returnedCampaigns).extracting("spending").contains(0.0);
    }

    private void createCampaign(CampaignParam campaignParam){
        RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/campaigns")
                .request()
                .post(Entity.entity(campaignParam, MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    void testSuccessfulBid(){
        CampaignParam campaignParam = new CampaignParam("Test Campaign 111", new String[]{"Keyword 1"}, 55.0);
        createCampaign(campaignParam);

        BidParam bidParam = new BidParam(5, new String[]{"Keyword 1"});
        Response response = createBid(bidParam);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        BidResult bidResult = response.readEntity(BidResult.class);
        assertThat(bidResult.getBidAmount()).isEqualTo(1);
        assertThat(bidResult.getBidId()).isEqualTo(5);
    }

    @Test
    void testUnsuccessfulBid() {
        BidParam bidParam = new BidParam(1, new String[]{"Non Existent Keyword"});
        Response response = createBid(bidParam);
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
    }

    @Test
    void testCampaignBalance(){
        CampaignParam campaignParam = new CampaignParam("Campaign Balance", new String[]{"Campaign Balance Keyword"}, 1.0);
        createCampaign(campaignParam);

        BidParam bidParam = new BidParam(6, new String[]{"Campaign Balance Keyword"});
        Response response = createBid(bidParam);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

        response = createBid(bidParam);
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
    }

    @Test
    void testMaximumSpendingPer10Sec(){
        CampaignParam campaignParam = new CampaignParam("Maximum Spending", new String[]{"Maximum Spending Keyword"}, 400.0);
        createCampaign(campaignParam);

        BidParam bidParam = new BidParam(6, new String[]{"Maximum Spending Keyword"});
        Response[] successfulBidResponses = new Response[10];
        for (int i = 0; i < 10; i++) {
            successfulBidResponses[i] = createBid(bidParam);
        }
        Response failedBidResponse = createBid(bidParam);

        assertThat(successfulBidResponses).extracting("statusInfo").containsOnly(Response.Status.OK);
        assertThat(failedBidResponse.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
    }

    private Response createBid(BidParam bidParam){
        return RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/bids")
                .request()
                .post(Entity.entity(bidParam, MediaType.APPLICATION_JSON_TYPE));
    }
}