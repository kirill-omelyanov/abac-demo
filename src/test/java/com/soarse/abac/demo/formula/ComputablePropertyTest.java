package com.soarse.abac.demo.formula;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soarse.abac.demo.AbstractAbacDemoTest;
import com.soarse.abac.demo.service.EmployeeService;
import com.soarse.common.util.JsonUtils;
import com.soarse.formula.service.computations.ComputableFieldInjectionService;
import com.soarse.formula.service.computations.ComputationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * Test demonstrating the functionality of computable properties
 * provided by the formula-spring-boot-starter library.
 */
@Slf4j
@Sql("/db/createEmployees.sql")
@Sql(value = "/db/truncateTables.sql", executionPhase = AFTER_TEST_METHOD)
public class ComputablePropertyTest extends AbstractAbacDemoTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ComputationService computationService;

    @Autowired
    private ComputableFieldInjectionService computableFieldInjectionService;

    /**
     * Calculation of the formula value in the service memory without a database call.
     * The formula defines what part of the salary corresponds to one user's competence.
     */
    @Test
    void inMemoryComputation() {

        // Set the user, as the formula depends on user attributes
        setUser(CHIEF_EDITOR);

        // Read the formula of the calculated value
        var formula = readFormula("/formula/property/salaryBySkill.json");

        // Calculate what part of the salary corresponds to one user's skill
        var value = computationService.computeValue(formula, BigDecimal.class);

        // Log the received value
        log.debug("Computed value: {}", value);

        // Ensure the result matches expectations
        var expectedValue = new BigDecimal(125000);

        assertThat(value).isNotNull();
        assertThat(value.compareTo(expectedValue)).isEqualTo(0);
    }

    /**
     * We will calculate an array of deviations of employees' salaries from the average salary in the employee's department.
     */
    @Test
    void salaryDivergenceArrayComputation() {

        // Set the user, as the formula may depend on user attributes
        setUser(ADMIN);

        // Read the formula of the computed array
        var formula = readFormula("/formula/property/salaryDivergenceArrayFormula.json");

        // Calculate the array of salary deviations
        var array = computationService.computeArray(formula, Long.class);

        // Log the received array
        log.debug("Computed mean divergence array: {}", array);

        // Ensure the result matches expectations
        assertThat(array).containsExactlyInAnyOrder(113333L, -26667L, -86667L, 70000L, -70000L);
    }

    /**
     * Add to the employee fields a computable field 'divergence', the formula of which
     * defines the deviation of an employee's salary from the average salary in the department.
     */
    @Test
    void salaryDivergencePropertyComputation() {

        // Set the user, as the formula may depend on user attributes
        setUser(ADMIN);

        // Set the access policies to actions and data for the admin
        setUniversalPolicies("/policy/universal/adminActionPolicy.json", "/policy/universal/adminDataPolicy.json");

        // Request a list of all employees
        var employees = employeeService.fetchAll();

        // Read the formula of the computed field
        var formulas = readComputationalPropertyFormulas("/formula/property/salaryDivergencePropertyFormula.json");

        // Add a computed field for each employee to the document with their data
        var enrichedDocuments = employees.stream()
                .map(JsonUtils::toObjectNode)
                .peek(document -> computableFieldInjectionService.injectComputableFields(document, formulas))
                .toList();

        // Log the received list of documents
        log.debug("Enriched employee documents:\n{}", enrichedDocuments.stream().map(ObjectNode::toPrettyString).collect(joining(",\n")));

        // Ensure the result matches expectations
        assertThat(enrichedDocuments)
                .map(document -> document.get("divergence").asInt())
                .containsExactlyInAnyOrder(113333, -26667, -86667, 70000, -70000);

        assertThat(enrichedDocuments)
                .filteredOn(document -> document.get("name").asText().equals("John"))
                .map(document -> document.get("divergence").asInt())
                .containsExactly(113333);

        assertThat(enrichedDocuments)
                .filteredOn(document -> document.get("branch").asText().equals(LA_BRANCH))
                .map(document -> document.get("divergence").asInt())
                .containsOnly(70000, -70000);
    }
}
