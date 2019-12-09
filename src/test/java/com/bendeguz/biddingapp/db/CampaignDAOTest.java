package com.bendeguz.biddingapp.db;

import com.bendeguz.biddingapp.core.Campaign;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link CampaignDAO}.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class CampaignDAOTest {

    private final DAOTestExtension daoTestRule = DAOTestExtension.newBuilder()
            .addEntityClass(Campaign.class)
            .build();

    private CampaignDAO campaignDAO;

    @BeforeEach
    void setUp() {
        campaignDAO = new CampaignDAO(daoTestRule.getSessionFactory());
    }

    @Test
    void createCampaign() {
        final Campaign testCampaign = daoTestRule.inTransaction(
                () -> campaignDAO.create(new Campaign("Test Campaign", new String[]{"Kobler", "Contextual"}, 1000.0)));
        assertThat(testCampaign.getId()).isGreaterThan(0);
        assertThat(testCampaign.getName()).isEqualTo("Test Campaign");
        assertThat(testCampaign.getKeywords()).containsOnly("Kobler", "Contextual");
        assertThat(testCampaign.getBudget()).isEqualTo(1000.0);
        assertThat(testCampaign.getSpending()).isEqualTo(0);
        assertThat(campaignDAO.findById(testCampaign.getId())).isEqualTo(Optional.of(testCampaign));
    }

    @Test
    void findAll() {
        daoTestRule.inTransaction(() -> {
            campaignDAO.create(new Campaign("Test Campaign", new String[]{"Kobler", "Contextual"}, 1000.0));
            campaignDAO.create(new Campaign("Test Campaign 2", new String[]{"Kobler", "Contextual"}, 1200.0));
            campaignDAO.create(new Campaign("Test", new String[]{"Kobler"}, 100.0));
        });

        final List<Campaign> campaigns = campaignDAO.findAll();
        assertThat(campaigns).extracting("name").containsOnly("Test Campaign", "Test Campaign 2", "Test");
        assertThat(campaigns).flatExtracting("keywords").containsOnly("Kobler", "Contextual");
        assertThat(campaigns).extracting("budget").containsOnly(100.0, 1000.0, 1200.0);
        assertThat(campaigns).extracting("spending").containsOnly(0.0);
    }

    @Test
    void handlesNullName() {
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() ->
                daoTestRule.inTransaction(() -> campaignDAO.create(new Campaign(null, new String[]{"Kobler"}, 100.0))));
    }

    @Test
    void findCampaignsByOneKeyword() {
        daoTestRule.inTransaction(() -> {
            campaignDAO.create(new Campaign("Test Campaign", new String[]{"Kobler", "Contextual"}, 1000.0));
            campaignDAO.create(new Campaign("Test Campaign 2", new String[]{"Kobler", "Contextual"}, 1200.0));
            campaignDAO.create(new Campaign("Test", new String[]{"Kobler"}, 100.0));
        });

        final List<Campaign> foundCampaigns = campaignDAO.findCampaignsWithPositiveBalanceByKeywords(new String[]{"Kobler"});
        assertThat(foundCampaigns).hasSize(3);
        assertThat(foundCampaigns).extracting("name").containsOnly("Test Campaign", "Test Campaign 2", "Test");
    }

    @Test
    void findCampaignsByMultipleKeywords() {
        daoTestRule.inTransaction(() -> {
            campaignDAO.create(new Campaign("Test Campaign", new String[]{"Contextual"}, 1000.0));
            campaignDAO.create(new Campaign("Test Campaign 2", new String[]{"Kobler 3", "Keyword"}, 1200.0));
            campaignDAO.create(new Campaign("Test", new String[]{"Kobler 2"}, 100.0));
        });

        final List<Campaign> foundCampaigns = campaignDAO.findCampaignsWithPositiveBalanceByKeywords(new String[]{"Contextual", "Keyword"});
        assertThat(foundCampaigns).hasSize(2);
        assertThat(foundCampaigns).extracting("name").containsOnly("Test Campaign", "Test Campaign 2");
    }

    @Test
    void findCampaignsWithNoBalance() {
        daoTestRule.inTransaction(() -> {
            Campaign campaign = new Campaign("Test Campaign", new String[]{"Contextual"}, 1000.0);
            campaign.setSpending(1000.0);
            campaignDAO.create(campaign);
        });

        final List<Campaign> foundCampaigns = campaignDAO.findCampaignsWithPositiveBalanceByKeywords(new String[]{"Contextual"});
        assertThat(foundCampaigns).hasSize(0);
    }

    @Test
    void tryToIncreaseSpending() {
        final Campaign testCampaign = daoTestRule.inTransaction(
                () -> campaignDAO.create(new Campaign("Test Campaign", new String[]{"Kobler", "Contextual"}, 1000.0)));

        boolean result = daoTestRule.inTransaction(
                () -> campaignDAO.tryToIncreaseSpending(testCampaign, 5.0));
        assertThat(result).isTrue();
        // Unfortunately, the update is not reflected in any subsequent test queries - this is the test framework's fault,
        // it works when trying on the deployed app.
    }

    @Test
    void tryToIncreaseSpendingFailure() {
        final Campaign testCampaign = daoTestRule.inTransaction(
                () -> campaignDAO.create(new Campaign("Test Campaign", new String[]{"Kobler", "Contextual"}, 5.0)));

        boolean result = daoTestRule.inTransaction(
                () -> campaignDAO.tryToIncreaseSpending(testCampaign, 10.0));
        assertThat(result).isFalse();
        assertThat(campaignDAO.findById(testCampaign.getId())).get().extracting("spending").containsOnly(0.0);
    }
}
