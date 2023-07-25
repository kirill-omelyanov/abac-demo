package com.soarse.abac.demo.service;

import com.soarse.abac.demo.AbstractAbacDemoTest;
import com.soarse.abac.demo.model.Employee;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.soarse.abac.model.action.ActionEffect.READ;
import static com.soarse.abac.model.action.ActionEffect.WRITE;
import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * Test verifying the correctness of ABAC access policies to the employee table.
 */
@Slf4j
@Sql("/db/createEmployees.sql")
@Sql(value = "/db/truncateTables.sql", executionPhase = AFTER_TEST_METHOD)
public class EmployeeServiceTest extends AbstractAbacDemoTest {

    @Autowired
    private EmployeeService employeeService;

    /**
     * By default, access to actions is denied.
     * <p>
     * Mike, a NY journalist, does not see any records.
     */
    @Test
    void actionAccessDeniedByDefault() {

        // Specify the user making the request
        setUser(JOURNALIST);

        // Request a list of all employees
        assertThatThrownBy(() -> employeeService.fetchAll())
                .isExactlyInstanceOf(AccessDeniedException.class);
    }

    /**
     * By default, access to data is denied.
     */
    @Test
    void dataAccessDeniedByDefault() {

        // Specify the user making the request
        setUser(ADMIN);

        // Set the access policy to actions for admin
        setUniversalPolicies("/policy/universal/adminActionPolicy.json");

        // Request a list of all employees
        var employees = employeeService.fetchAll();

        // Ensure the result meets expectations
        assertThat(employees).hasSize(0);
    }

    /**
     * Admin rights allow you to see all employees.
     */
    @Test
    public void adminCanReadAllEntries() {

        // Specify the user making the request
        setUser(ADMIN);

        // Set access policies for actions and data for admin
        setUniversalPolicies("/policy/universal/adminActionPolicy.json", "/policy/universal/adminDataPolicy.json");

        // Request a list of all employees
        var employees = employeeService.fetchAll();

        // Log the received list
        logEmployees(employees);

        // Ensure the result meets expectations
        assertThat(employees).hasSize(5);
    }

    /**
     * Nichol, a LA chief editor:
     * - sees all LA employees and does not see NY employees
     * - sees only the fields ID, name, branch, roles, skills set, and salary
     */
    @Test
    public void editorCanReadAllLAEntries() {

        // Specify the user making the request
        setUser(CHIEF_EDITOR);

        // Set the reading policy for the list of employees for the editor
        setTableEntityPolicies(READ, EMPLOYEE_TABLE_ID, "/policy/employee/editorEmployeeReadPolicy.json");

        // Request a list of all employees
        var employees = employeeService.fetchAll();

        // Log the received list
        logEmployees(employees);

        // Ensure the result meets expectations
        assertThat(employees).hasSize(2);
        assertThat(employees).map(Employee::getBranch).containsOnly(LA_BRANCH);

        // This field is not visible
        assertThat(employees).map(Employee::getPassport).allMatch(Objects::isNull);

        // These fields are visible
        assertThat(employees).map(Employee::getId).allMatch(field -> !isNull(field));
        assertThat(employees).map(Employee::getName).allMatch(field -> !isNull(field));
        assertThat(employees).map(Employee::getRoles).allMatch(list -> !list.isEmpty());
        assertThat(employees).map(Employee::getSkills).allMatch(list -> !list.isEmpty());
        assertThat(employees).map(Employee::getSalary).allMatch(field -> !isNull(field));
    }

    /**
     * Nichol, a LA chief editor, cannot add an employee to the NY branch.
     */
    @Test
    public void editorCantAddEmployeeToNewYorkBranch() {

        // Specify the user making the request
        setUser(CHIEF_EDITOR);

        // Set the modification policy for the list of employees for the editor
        setTableEntityPolicies(WRITE, EMPLOYEE_TABLE_ID, "/policy/employee/editorEmployeeWritePolicy.json");

        // Create an object with the data of the new employee
        var employeeDTO = Employee.builder().branch(NY_BRANCH).name("Helen").salary(120000).build();

        // Try to create an employee
        assertThatThrownBy(() -> employeeService.create(employeeDTO))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessageMatching("Creation of the record .* by action abac-demo.EmployeeService.create is forbidden in current context.");
    }

    /**
     * Nichol, the chief editor from LA, cannot add an employee to the LA branch with a salary of 140,000 USD.
     */
    @Test
    public void editorCantAddEmployeeWithTooHighSalary() {

        // Specify the user performing the request
        setUser(CHIEF_EDITOR);

        // Set up the policy for modifying the employee list for the chief editor
        setTableEntityPolicies(WRITE, EMPLOYEE_TABLE_ID, "/policy/employee/editorEmployeeWritePolicy.json");

        // Create an object with the new employee's data
        var employeeDTO = Employee.builder().branch(LA_BRANCH).name("Helen").salary(140000).build();

        // Try to create an employee
        assertThatThrownBy(() -> employeeService.create(employeeDTO))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessageMatching("Creation of the record .* by action abac-demo.EmployeeService.create is forbidden in current context.");
    }

    /**
     * Nichol, the chief editor from LA:
     * - can add an employee to the LA branch with a salary of 120,000 USD
     * - cannot increase the salary of a new employee to 140,000 USD., but can increase it to 130,000 USD
     */
    @Test
    public void editorCanAddEmployeeWithAppropriateSalaryToLosAngelesBranch() {

        // Specify the user performing the request
        setUser(CHIEF_EDITOR);

        // Set up the policy for modifying the employee list for the chief editor
        setTableEntityPolicies(WRITE, EMPLOYEE_TABLE_ID, "/policy/employee/editorEmployeeWritePolicy.json");

        // Create an object with the new employee's data
        var employeeDTO = Employee.builder().branch(LA_BRANCH).name("Helen").salary(120000).build();

        // Try to create an employee
        var employee = employeeService.create(employeeDTO);
        var employeeId = employee.getId();

        // Make sure the new employee has been created
        assertThat(employee).isNotNull();
        assertThat(employeeId).isNotNull();
        assertThat(employee.getBranch()).isEqualTo(LA_BRANCH);
        assertThat(employee.getName()).isEqualTo("Helen");
        assertThat(employee.getSalary()).isEqualTo(120000);

        // Try to increase the salary to 140,000 USD.
        employeeDTO.setSalary(140000);

        assertThatThrownBy(() -> employeeService.update(employeeId, employeeDTO))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessageMatching("Update of the record .* by action abac-demo.EmployeeService.update is forbidden in current context.");

        // Try to increase the salary to 130,000 USD.
        employeeDTO.setSalary(130000);

        var updatedEmployee = employeeService.update(employeeId, employeeDTO);

        // Make sure the change was successful
        assertThat(updatedEmployee).isNotNull();
        assertThat(updatedEmployee.getBranch()).isEqualTo(LA_BRANCH);
        assertThat(updatedEmployee.getName()).isEqualTo("Helen");
        assertThat(updatedEmployee.getSalary()).isEqualTo(130000);
    }

    /**
     * Nichol, the chief editor from LA:
     *   - cannot remove an employee from another branch
     *   - can remove an employee from his branch
     */
    @Test
    public void editorCantDeleteEmployeeInLosAngelesBranch() {

        // Employee IDs for removal
        var newYorkBranchEmployeeId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        var losAngelesBranchEmployeeId = UUID.fromString("00000000-0000-0000-0000-000000000005");

        // Specify the user executing the request
        setUser(CHIEF_EDITOR);

        // Set read and write policies for the chief editor
        setTableEntityPolicies(READ, EMPLOYEE_TABLE_ID, "/policy/employee/editorEmployeeReadPolicy.json");
        setTableEntityPolicies(WRITE, EMPLOYEE_TABLE_ID, "/policy/employee/editorEmployeeWritePolicy.json");

        // Make sure there are initially two employees in LA
        var employees = employeeService.fetchAll();

        // Output the obtained list to the log
        logEmployees(employees);
        assertThat(employees).hasSize(2);
        assertThat(employees).map(Employee::getId).contains(losAngelesBranchEmployeeId);

        // Try to remove a NY employee
        assertThatThrownBy(() -> employeeService.delete(newYorkBranchEmployeeId))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessageMatching("Deletion of the record .* by action abac-demo.EmployeeService.delete is forbidden in current context.");

        // Try to remove a LA employee
        employeeService.delete(losAngelesBranchEmployeeId);

        // Check that the employee has been removed
        var downsizedEmployees = employeeService.fetchAll();

        logEmployees(downsizedEmployees);
        assertThat(downsizedEmployees).hasSize(1);
        assertThat(downsizedEmployees).map(Employee::getId).doesNotContain(losAngelesBranchEmployeeId);
    }

    /**
     * Peter, the accountant:
     * - sees all employees
     * - only sees ID, name, branch, passport number, and salary fields
     */
    @Test
    public void accountantCanReadAllEntries() {

        // Specify the user executing the request
        setUser(ACCOUNTANT);

        // Set the read policy for the accountant
        setTableEntityPolicies(READ, EMPLOYEE_TABLE_ID, "/policy/employee/accountantEmployeeReadPolicy.json");

        // Request a list of all employees
        var employees = employeeService.fetchAll();

        // Output the obtained list to the log
        logEmployees(employees);

        // Make sure the result meets expectations
        assertThat(employees).hasSize(5);

        // These fields are not visible
        assertThat(employees).map(Employee::getRoles).allMatch(List::isEmpty);
        assertThat(employees).map(Employee::getSkills).allMatch(List::isEmpty);

        // These fields are visible
        assertThat(employees).map(Employee::getId).allMatch(field -> !isNull(field));
        assertThat(employees).map(Employee::getName).allMatch(field -> !isNull(field));
        assertThat(employees).map(Employee::getBranch).allMatch(field -> !isNull(field));
        assertThat(employees).map(Employee::getPassport).allMatch(field -> !isNull(field));
        assertThat(employees).map(Employee::getSalary).allMatch(field -> !isNull(field));
    }

    /**
     * Nichol, the chief editor from LA, can't add an employee to the NY branch.
     */
    @Test
    public void accountantCantUpdateOrDeleteAnyEmployee() {

        // Specify the user executing the request
        setUser(ACCOUNTANT);

        // Set the read policy for the accountant
        setTableEntityPolicies(READ, EMPLOYEE_TABLE_ID, "/policy/employee/accountantEmployeeReadPolicy.json");

        // Request a list of all employees
        var employees = employeeService.fetchAll();

        var employeeDTO = Employee.builder().branch(LA_BRANCH).name("Helen").salary(120000).build();

        // Try to update at least some employee
        employees.forEach(employee ->
                assertThatThrownBy(() -> employeeService.update(employee.getId(), employeeDTO))
                        .isExactlyInstanceOf(AccessDeniedException.class)
        );

        // Try to delete at least some employee
        employees.forEach(employee ->
                assertThatThrownBy(() -> employeeService.delete(employee.getId()))
                        .isExactlyInstanceOf(AccessDeniedException.class)
        );
    }
}
