package com.soarse.abac.demo.model;

import com.soarse.abac.model.entity.FilterableTableEntity;
import com.soarse.common.model.Identifiable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;


/**
 * Employee of the Logos Media Holding
 */
@Data
@FieldDefaults(level = PRIVATE)
public class Employee implements FilterableTableEntity, Identifiable {

    /**
     * Unique ID
     */
    UUID id;

    /**
     * Employee's name
     */
    String name;

    /**
     * Holding's branch
     */
    String branch;

    /**
     * Employee's roles
     */
    List<String> roles;

    /**
     * List of competencies
     */
    List<String> skills;

    /**
     * Passport number
     */
    String passport;

    /**
     * Salary in rubles
     */
    Integer salary;

    @Builder
    public Employee(UUID id, String name, String branch, List<String> roles, List<String> skills, String passport, Integer salary) {

        this.id = id;
        this.name = name;
        this.branch = branch;
        this.roles = roles != null ? roles : emptyList();
        this.skills = skills != null ? skills : emptyList();
        this.passport = passport;
        this.salary = salary;
    }

    public void setRoles(List<String> roles) {

        this.roles = roles != null ? roles : emptyList();
    }

    public void setSkills(List<String> skills) {

        this.skills = skills != null ? skills : emptyList();
    }

    @Override
    public String toString() {

        return "Employee(%s, %s, %s, %-16s, %-25s, %s, %s)".formatted(id, name, branch, roles, skills, passport, salary);
    }
}
