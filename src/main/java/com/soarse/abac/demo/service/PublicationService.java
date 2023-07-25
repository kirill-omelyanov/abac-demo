package com.soarse.abac.demo.service;

import com.soarse.abac.annotation.filtration.TableFiltrationPoint;
import com.soarse.abac.annotation.filtration.write.Create;
import com.soarse.abac.annotation.filtration.write.Delete;
import com.soarse.abac.annotation.filtration.write.Update;
import com.soarse.abac.annotation.permission.TablePermissionPoint;
import com.soarse.abac.annotation.permission.TablePermissionPointService;
import com.soarse.abac.builder.condition.AbacConditionBuilder;
import com.soarse.abac.demo.model.Publication;
import com.soarse.common.annotation.Aim;
import com.soarse.formula.Formula;
import com.soarse.formula.service.FiltrationFormulaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.soarse.abac.demo.generated.jooq.Tables.PUBLICATION;
import static com.soarse.abac.model.action.ActionEffect.WRITE;

/**
 * Service for managing the publication list.
 */
@Slf4j
@RequiredArgsConstructor
@TableFiltrationPoint(schema = "abac_demo", table = "publication")
@TablePermissionPointService(schema = "abac_demo", table = "publication")
public class PublicationService {

    private final DSLContext dsl;
    private final AbacConditionBuilder abacConditionBuilder;
    private final FiltrationFormulaService filtrationFormulaService;

    /**
     * Returns a publication by ID.
     */
    @Transactional(readOnly = true)
    @TablePermissionPoint(id = "PublicationService.fetchOne", title = "Fetching a publication by ID")
    public Publication fetchOne(UUID id) {

        // Form a condition that implements the ABAC filtering function
        var abacCondition = abacConditionBuilder.build();

        // Get a list of all entries available to the user
        return dsl.selectFrom(PUBLICATION)
                .where(PUBLICATION.ID.eq(id), abacCondition)
                .fetchOptional()
                .map(record -> record.into(Publication.class))
                .orElseThrow(() -> new RuntimeException("Failed to find publication with ID: %s".formatted(id)));
    }

    /**
     * Returns the full list of publications.
     */
    @Transactional(readOnly = true)
    @TablePermissionPoint(id = "PublicationService.fetchAll", title = "Fetching all publications")
    public List<Publication> fetchAll() {

        // Form a condition that implements the ABAC filtering function
        var abacCondition = abacConditionBuilder.build();

        // Get a list of all entries available to the user
        return dsl.selectFrom(PUBLICATION)
                .where(abacCondition)
                .fetchInto(Publication.class);
    }

    /**
     * Returns a list of publications by filter.
     */
    @Transactional(readOnly = true)
    @TablePermissionPoint(id = "PublicationService.fetchAllByFilter", title = "Retrieving all publications by filter")
    public List<Publication> fetchAllByFilter(Formula filter) {

        // Form a condition based on the specified user filter
        var filterCondition = filtrationFormulaService.toCondition(filter);

        log.debug("Filter condition:\n{}", filterCondition);

        // Form a condition that implements the ABAC filtering function
        var abacCondition = abacConditionBuilder.build();

        // If necessary, the condition can be used without jOOQ
        log.debug("ABAC SQL condition:\n{}", abacCondition);

        // Get a list of all entries that meet the conditions
        return dsl.selectFrom(PUBLICATION)
                .where(filterCondition, abacCondition)
                .fetchInto(Publication.class);
    }

    /**
     * Creates a publication.
     */
    @Create
    @TablePermissionPoint(id = "PublicationService.create", title = "Creating a publication.", effect = WRITE)
    public Publication create(Publication dto) {

        log.debug("Creating the publication: {}", dto);

        // Generate a random ID
        var id = UUID.randomUUID();

        // Create a record in the publication table
        var publication = dsl.insertInto(PUBLICATION)
                .set(PUBLICATION.ID, id)
                .set(PUBLICATION.BRANCH, dto.getBranch())
                .set(PUBLICATION.THEME, dto.getTheme())
                .set(PUBLICATION.AUTHOR_ID, dto.getAuthorId())
                .set(PUBLICATION.STATUS, dto.getStatus())
                .set(PUBLICATION.TITLE, dto.getTitle())
                .returning()
                .fetchOptional()
                .map(record -> record.into(Publication.class))
                .orElseThrow(() -> new RuntimeException("Error creating publication: %s".formatted(dto)));

        log.debug("Publication created: {}", publication);

        return publication;
    }

    /**
     * Modifies the publication with the specified ID according to the given data.
     */
    @Update
    @TablePermissionPoint(id = "PublicationService.update", title = "Updating the publication", effect = WRITE)
    public Publication update(@Aim UUID id, Publication dto) {

        log.debug("Modifying the publication with ID {} with the following data: {}", id, dto);

        // Modify the record with the specified id the publication table
        var publication = dsl.update(PUBLICATION)
                .set(PUBLICATION.BRANCH, dto.getBranch())
                .set(PUBLICATION.THEME, dto.getTheme())
                .set(PUBLICATION.AUTHOR_ID, dto.getAuthorId())
                .set(PUBLICATION.STATUS, dto.getStatus())
                .set(PUBLICATION.TITLE, dto.getTitle())
                .where(PUBLICATION.ID.eq(id))
                .returning()
                .fetchOptional()
                .map(record -> record.into(Publication.class))
                .orElseThrow(() -> new RuntimeException("Error updating publication with ID %s with data: %s".formatted(id, dto)));

        log.debug("Publication updated: {}", publication);

        return publication;
    }

    /**
     * Sets the publication display date on the information portal.
     *
     * @param id              Publication ID
     * @param publicationDate Planned publication display date on the information portal
     */
    @Update
    @TablePermissionPoint(id = "PublicationService.setPublicationDate", title = "Set the publication display date on the information portal", effect = WRITE)
    public Publication setPublicationDate(@Aim UUID id, LocalDate publicationDate) {

        log.debug("Setting the display date on the information portal for the publication with ID {} to {}", id, publicationDate);

        // Modify the record with the specified id in the publication table
        var publication = dsl.update(PUBLICATION)
                .set(PUBLICATION.PUBLICATION_DATE, publicationDate)
                .where(PUBLICATION.ID.eq(id))
                .returning()
                .fetchOptional()
                .map(record -> record.into(Publication.class))
                .orElseThrow(() -> new RuntimeException("Error changing the publication date for the record with ID %s to: %s".formatted(id, publicationDate)));

        log.debug("Publication date changed: {}", publication);

        return publication;
    }

    /**
     * Deletes the publication with the specified ID.
     */
    @Delete
    @TablePermissionPoint(id = "PublicationService.delete", title = "Delete publication", effect = WRITE)
    public void delete(UUID id) {

        log.debug("Deleting the publication with ID {}", id);

        // Delete the record by ID
        dsl.deleteFrom(PUBLICATION)
                .where(PUBLICATION.ID.eq(id))
                .execute();

        log.debug("The publication with ID {} has been deleted", id);
    }
}
