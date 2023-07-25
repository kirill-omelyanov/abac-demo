package com.soarse.abac.demo.formula;

import com.soarse.abac.demo.AbstractAbacDemoTest;
import com.soarse.abac.demo.model.Publication;
import com.soarse.abac.demo.service.PublicationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import static java.time.LocalDate.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * Test demonstrating the functionality of universal filtering
 * provided by the formula-spring-boot-starter library.
 */
@Slf4j
@Sql(value = {"/db/createEmployees.sql", "/db/createPublications.sql"})
@Sql(value = "/db/truncateTables.sql", executionPhase = AFTER_TEST_METHOD)
public class UniversalFiltrationTest extends AbstractAbacDemoTest {

    @Autowired
    private PublicationService publicationService;

    /**
     * Get a list of publications by a custom user filter, selecting publications
     * that either have a theme from the list ["Education", "Politics"], or the status is "Published".
     */
    @Test
    void readPublicationsByThemeAndStatusFilter() {

        // Set the user as admin, so ABAC restrictions do not reduce the list of publications issued
        setUser(ADMIN);

        // Set the access policies to actions and data for the admin
        setUniversalPolicies("/policy/universal/adminActionPolicy.json", "/policy/universal/adminDataPolicy.json");

        // Read the custom user filter
        var filter = readFormula("/formula/filter/themeAndStatusFilter.json");

        // Request a list of all publications by filter
        var publications = publicationService.fetchAllByFilter(filter);

        // Log the received list
        logPublications(publications);

        // Ensure the result matches expectations
        assertThat(publications).hasSize(3);
    }

    /**
     * Get a list of publications by a custom user filter,
     * selecting publications only from the user's department and with a publication date in the past.
     */
    @Test
    void readPublicationsByUserBranchAndPublicationDateFilter() {

        // Set the user as admin, so ABAC restrictions do not reduce the list of publications issued
        setUser(ADMIN);

        // Set the access policies to actions and data for the admin
        setUniversalPolicies("/policy/universal/adminActionPolicy.json", "/policy/universal/adminDataPolicy.json");

        // Read the custom user filter
        var filter = readFormula("/formula/filter/userBranchAndPublicationDateFilter.json");

        // Request a list of all publications by filter
        var publications = publicationService.fetchAllByFilter(filter);

        // Log the received list
        logPublications(publications);

        // Ensure the result matches expectations
        assertThat(publications).hasSize(2);
        assertThat(publications).map(Publication::getBranch).containsOnly(NY_BRANCH);
        assertThat(publications).map(Publication::getPublicationDate).containsOnly(of(2023, 5,25));
    }
}
