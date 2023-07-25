package com.soarse.abac.demo.service;

import com.soarse.abac.demo.AbstractAbacDemoTest;
import com.soarse.abac.demo.model.Publication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.jdbc.Sql;

import static com.soarse.abac.model.action.ActionEffect.READ;
import static com.soarse.abac.model.action.ActionEffect.WRITE;
import static java.time.LocalDate.of;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * Test that checks the correct functioning of ABAC access policies to the publication table.
 */
@Slf4j
@Sql(value = {"/db/createEmployees.sql", "/db/createPublications.sql"})
@Sql(value = "/db/truncateTables.sql", executionPhase = AFTER_TEST_METHOD)
class PublicationServiceTest extends AbstractAbacDemoTest {

    @Autowired
    private PublicationService publicationService;

    /**
     * By default, access to actions is prohibited.
     * 
     * <p>
     * Peter, an accountant, does not see a single record.
     */
    @Test
    void actionAccessDeniedByDefault() {

        // Specify the user making the request
        setUser(ACCOUNTANT);

        // Request a list of all publications
        assertThatThrownBy(() -> publicationService.fetchAll())
                .isExactlyInstanceOf(AccessDeniedException.class);
    }

    /**
     * Admin rights allow you to see all publications.
     */
    @Test
    public void adminCanReadAllEntries() {

        // Specify the user making the request
        setUser(ADMIN);

        // Set access policies to actions and data for admin
        setUniversalPolicies("/policy/universal/adminActionPolicy.json", "/policy/universal/adminDataPolicy.json");

        // Request a list of all publications
        var publications = publicationService.fetchAll();

        // Log the received list
        logPublications(publications);

        // Make sure the result matches expectations
        assertThat(publications).hasSize(5);
    }

    /**
     * Nichol, the LA editor-in-chief, can view all articles from his branch.
     */
    @Test
    public void editorCanReadAllLAEntries() {

        // Specify the user making the request
        setUser(CHIEF_EDITOR);

        // Set the policy of reading the list of publications for the editor-in-chief
        setTableEntityPolicies(READ, PUBLICATION_TABLE_ID, "/policy/publication/editorPublicationReadPolicy.json");

        // Request a list of all publications
        var publications = publicationService.fetchAll();

        // Log the received list
        logPublications(publications);

        // Make sure the result matches expectations
        assertThat(publications).hasSize(3);
        assertThat(publications).map(Publication::getBranch).containsOnly(LA_BRANCH);
    }

    /**
     * Nichol, the LA editor-in-chief, can modify all articles from his branch.
     */
    @Test
    public void editorCanUpdateLAEntry() {

        // ID of the publication for modification
        var laBranchPublicationId = fromString("00000001-0000-0000-0000-000000000002");

        // Specify the user making the request
        setUser(CHIEF_EDITOR);

        // Set the policies for reading and modifying the list of publications for the editor-in-chief
        setTableEntityPolicies(READ, PUBLICATION_TABLE_ID, "/policy/publication/editorPublicationReadPolicy.json");
        setTableEntityPolicies(WRITE, PUBLICATION_TABLE_ID, "/policy/publication/editorPublicationWritePolicy.json");

        // Get the required publication
        var publication = publicationService.fetchOne(laBranchPublicationId);

        assertThat(publication.getBranch()).isEqualTo(LA_BRANCH);
        assertThat(publication.getStatus()).isEqualTo("In progress");

        // Modify the publication
        publication.setStatus("Ready");

        var updatedPublication = publicationService.update(laBranchPublicationId, publication);

        // Make sure the result matches expectations
        assertThat(updatedPublication.getStatus()).isEqualTo("Ready");
    }

    /**
     * Nichol, the chief editor from LA, can set the publication date for an article,
     * but for each date and topic in the department, there can be no more than one article.
     */
    @Test
    public void editorCanSetPublicationDateProperly() {

        // Publication ID for modification
        var publicationId = fromString("00000001-0000-0000-0000-000000000004");

        // Let's specify the user making the request
        setUser(CHIEF_EDITOR);

        // Set up read and write policies for the chief editor
        setTableEntityPolicies(READ, PUBLICATION_TABLE_ID, "/policy/publication/editorPublicationReadPolicy.json");
        setTableEntityPolicies(WRITE, PUBLICATION_TABLE_ID, "/policy/publication/editorPublicationWritePolicy.json");

        // Get the required publication
        var publication = publicationService.fetchOne(publicationId);

        assertThat(publication.getPublicationDate()).isNull();

        // Try to set a publication date that conflicts with the publication of another article
        assertThatThrownBy(() -> publicationService.setPublicationDate(publicationId, of(2023, 6, 30)))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessageMatching("Update of the record .* by action abac-demo.PublicationService.setPublicationDate is forbidden in current context.");

        // Set a publication date that does not conflict with the publications of other articles
        var updatedPublication = publicationService.setPublicationDate(publicationId, of(2023, 6, 10));

        // Make sure the result matches expectations
        assertThat(updatedPublication.getPublicationDate()).isEqualTo(of(2023, 6, 10));
    }

    /**
     * Mike, the NY journalist, only sees his own article.
     */
    @Test
    public void journalistCanReadOwnEntriesOnly() {

        // Set the user making the request
        setUser(JOURNALIST);

        // Set the read policy for the list of publications for the journalist
        setTableEntityPolicies(READ, PUBLICATION_TABLE_ID, "/policy/publication/journalistPublicationReadPolicy.json");

        // Request a list of all publications
        var publications = publicationService.fetchAll();

        // Output the obtained list to the log
        logPublications(publications);

        // Ensure the result matches expectations
        assertThat(publications).hasSize(1);
        assertThat(publications).map(Publication::getAuthorId).containsOnly(fromString("00000000-0000-0000-0000-000000000003"));
    }


    /**
     * Mike, the NY journalist:
     * - cannot create an article with the theme of Sport
     * - can create an article with the theme of Education
     */
    @Test
    public void journalistCanCreatePublicationAccordingToHisSkills() {

        // Set the user making the request
        setUser(JOURNALIST);
        var userId = fromString("00000000-0000-0000-0000-000000000003");

        // Set read and write policies for the list of publications for the journalist
        setTableEntityPolicies(READ, PUBLICATION_TABLE_ID, "/policy/publication/journalistPublicationReadPolicy.json");
        setTableEntityPolicies(WRITE, PUBLICATION_TABLE_ID, "/policy/publication/journalistPublicationWritePolicy.json");

        // Request a list of all publications
        var publications = publicationService.fetchAll();

        // Log the obtained list
        logPublications(publications);

        // Ensure the result matches expectations
        assertThat(publications).hasSize(1);

        // Attempt to create an article with the theme "Sports", which does not match the user's competence list
        var dto = Publication.builder().branch(NY_BRANCH).authorId(userId).theme("Sports").status("In progress").build();

        assertThatThrownBy(() -> publicationService.create(dto))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessageMatching("Creation of the record .* by action abac-demo.PublicationService.create is forbidden in current context.");

        // Create an article with the theme "Education", which matches the user's competence list
        dto.setTheme("Education");

        var publication = publicationService.create(dto);

        // Ensure the result matches expectations
        assertThat(publication).isNotNull();
        assertThat(publication.getBranch()).isEqualTo(NY_BRANCH);
        assertThat(publication.getAuthorId()).isEqualTo(userId);
        assertThat(publication.getTheme()).isEqualTo("Education");
        assertThat(publication.getStatus()).isEqualTo("In progress");

        // Request a list of all publications
        var updatedPublications = publicationService.fetchAll();

        // Log the obtained list
        logPublications(updatedPublications);
        assertThat(updatedPublications).hasSize(2);
    }
}