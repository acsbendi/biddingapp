package com.bendeguz.biddingapp.resources;

import com.bendeguz.biddingapp.api.CampaignParam;
import com.bendeguz.biddingapp.core.Campaign;
import com.bendeguz.biddingapp.db.CampaignDAO;
import com.google.common.collect.ImmutableList;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CampaignsResource}.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class CampaignsResourceTest {
    private static final CampaignDAO CAMPAIGN_DAO = mock(CampaignDAO.class);
    private static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .addResource(new CampaignsResource(CAMPAIGN_DAO))
            .build();
    private final ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
    private final ArgumentCaptor<Long> campaignIdCaptor = ArgumentCaptor.forClass(Long.class);
    private Campaign campaign1;
    private Campaign campaign2;

    @BeforeEach
    void setUp() {
        campaign1 = new Campaign();
        campaign1.setName("Test Campaign");
        campaign1.setKeywords(new HashSet<>(Collections.singletonList("Keyword 1")));
        campaign1.setBudget(100);
        campaign2 = new Campaign();
        campaign2.setName("Test Campaign 2");
        campaign2.setKeywords(new HashSet<>(Collections.singletonList("Keyword 2")));
        campaign2.setBudget(200);
    }

    @AfterEach
    void tearDown() {
        reset(CAMPAIGN_DAO);
    }

    @Test
    void createCampaign() {
        when(CAMPAIGN_DAO.create(any(Campaign.class))).thenReturn(campaign1);
        CampaignParam campaignParam = new CampaignParam("Test Campaign", new String[]{"Keyword 1"}, 100.0);
        final Response response = RESOURCES.target("/campaigns")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(campaignParam, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);
        verify(CAMPAIGN_DAO).create(campaignCaptor.capture());
        assertThat(campaignCaptor.getValue()).isEqualTo(campaign1);
    }

    @Test
    void listCampaigns() {
        final List<Campaign> campaigns = ImmutableList.of(campaign1, campaign2);
        when(CAMPAIGN_DAO.findAll()).thenReturn(campaigns);

        final List<Campaign> response = RESOURCES.target("/campaigns")
            .request().get(new GenericType<List<Campaign>>() {
            });

        verify(CAMPAIGN_DAO).findAll();
        assertThat(response).containsAll(campaigns);
    }

    @Test
    void getCampaign() {
        when(CAMPAIGN_DAO.findById(any(Long.class))).thenReturn(Optional.of(campaign1));
        final Response response = RESOURCES.target("/campaigns/" + campaign1.getId())
                .request()
                .get(new GenericType<Response>() {});

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        Campaign returnedCampaign = response.readEntity(Campaign.class);
        assertThat(returnedCampaign.getName()).isEqualTo("Test Campaign");
        assertThat(returnedCampaign.getKeywords()).containsOnly("Keyword 1");
        assertThat(returnedCampaign.getBudget()).isEqualTo(100);
        verify(CAMPAIGN_DAO).findById(campaignIdCaptor.capture());
        assertThat(campaignIdCaptor.getValue()).isEqualTo(campaign1.getId());
    }

    @Test
    void getCampaignNotFound() {
        when(CAMPAIGN_DAO.findById(any(Long.class))).thenReturn(Optional.empty());
        final Response response = RESOURCES.target("/campaigns/324234")
                .request()
                .get(new GenericType<Response>() {});

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        verify(CAMPAIGN_DAO).findById(campaignIdCaptor.capture());
        assertThat(campaignIdCaptor.getValue()).isEqualTo(324234);
    }
}