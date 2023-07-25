package com.soarse.abac.demo.model;

import com.soarse.common.model.Identifiable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

/**
 * Publication of the Logos Media Holding
 */
@Data
@Builder
@FieldDefaults(level = PRIVATE)
public class Publication implements Identifiable {

    /**
     * Unique ID
     */
    UUID id;

    /**
     * Holding's branch
     */
    String branch;

    /**
     * Publication topic
     */
    String theme;

    /**
     * Author's ID
     */
    UUID authorId;

    /**
     * Publication status
     */
    String status;

    /**
     * Date of the publication display on the information portal
     */
    LocalDate publicationDate;

    /**
     * Publication title
     */
    String title;

    @Override
    public String toString() {

        return "Publication(%s, %s, %s, %s, %s, %s, %s)".formatted(id, branch, theme, authorId, status, publicationDate, title);
    }
}
