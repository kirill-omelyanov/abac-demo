package com.soarse.abac.demo.service;

import com.soarse.abac.annotation.filtration.TableFiltrationPoint;
import com.soarse.abac.annotation.filtration.write.Create;
import com.soarse.abac.annotation.filtration.write.Delete;
import com.soarse.abac.annotation.filtration.write.Update;
import com.soarse.abac.annotation.permission.TablePermissionPoint;
import com.soarse.abac.annotation.permission.TablePermissionPointService;
import com.soarse.abac.builder.condition.AbacConditionBuilder;
import com.soarse.abac.demo.model.Employee;
import com.soarse.common.annotation.Aim;
import com.soarse.common.util.jooq.JooqUtils;
import com.soarse.formula.Formula;
import com.soarse.formula.service.FiltrationFormulaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.soarse.abac.demo.generated.jooq.Tables.EMPLOYEE;
import static com.soarse.abac.model.action.ActionEffect.WRITE;

/**
 * Service for managing the list of employees.
 */
@Slf4j
@RequiredArgsConstructor
@TableFiltrationPoint(schema = "abac_demo", table = "employee")
@TablePermissionPointService(schema = "abac_demo", table = "employee")
public class EmployeeService {

    private final DSLContext dsl;
    private final JooqUtils jooqUtils;
    private final AbacConditionBuilder abacConditionBuilder;
    private final FiltrationFormulaService filtrationFormulaService;

    /**
     * Returns the complete list of employees.
     */
    @Transactional(readOnly = true)
    @TablePermissionPoint(id = "EmployeeService.fetchAll", title = "Fetching all employees")
    public List<Employee> fetchAll() {

        // Form a condition that implements the ABAC filtering function
        var abacCondition = abacConditionBuilder.build();

        // Get a list of all entries available to the user
        return dsl.selectFrom(EMPLOYEE)
                .where(abacCondition)
                .fetchInto(Employee.class);
    }

    /**
     * Returns a list of employees by filter.
     */
    @Transactional(readOnly = true)
    @TablePermissionPoint(id = "EmployeeService.fetchAllByFilter", title = "Fetching all employees by filter")
    public List<Employee> fetchAllByFilter(Formula filter) {

        // Form a condition based on the specified user filter
        var filterCondition = filtrationFormulaService.toCondition(filter);

        // Form a condition that implements the ABAC filtering function
        var abacCondition = abacConditionBuilder.build();

        // If necessary, the condition can be used without jOOQ
        log.debug("ABAC SQL condition:\n{}", filterCondition.toString());

        // Get a list of all entries that meet the conditions
        return dsl.selectFrom(EMPLOYEE)
                .where(filterCondition, abacCondition)
                .fetchInto(Employee.class);
    }

    /**
     * Creates an employee.
     */
    @Create
    @TablePermissionPoint(id = "EmployeeService.create", title = "Creating an employee", effect = WRITE)
    public Employee create(Employee dto) {

        log.debug("Creating the employee: {}", dto);

        // Generate a random ID
        var id = UUID.randomUUID();

        // Create a record in the employee table
        var employee = dsl.insertInto(EMPLOYEE)
                .set(EMPLOYEE.ID, id)
                .set(EMPLOYEE.NAME, dto.getName())
                .set(EMPLOYEE.BRANCH, dto.getBranch())
                .set(EMPLOYEE.ROLES, jooqUtils.toNonNullJsonArray(dto.getRoles()))
                .set(EMPLOYEE.SKILLS, jooqUtils.toNonNullJsonArray(dto.getSkills()))
                .set(EMPLOYEE.PASSPORT, dto.getPassport())
                .set(EMPLOYEE.SALARY, dto.getSalary())
                .returning()
                .fetchOptional()
                .map(record -> record.into(Employee.class))
                .orElseThrow(() -> new RuntimeException("Error creating the employee: %s".formatted(dto)));

        log.debug("Employee created: {}", employee);

        return employee;
    }

    /**
     * Modifies the employee with the specified ID according to the given data.
     */
    @Update
    @TablePermissionPoint(id = "EmployeeService.update", title = "Updating the employee", effect = WRITE)
    public Employee update(@Aim UUID id, Employee dto) {

        log.debug("Modifying the employee with ID {} with the following data: {}", id, dto);

        // Modify the record with the specified id the employee table
        var employee = dsl.update(EMPLOYEE)
                .set(EMPLOYEE.NAME, dto.getName())
                .set(EMPLOYEE.BRANCH, dto.getBranch())
                .set(EMPLOYEE.ROLES, jooqUtils.toNonNullJsonArray(dto.getRoles()))
                .set(EMPLOYEE.SKILLS, jooqUtils.toNonNullJsonArray(dto.getSkills()))
                .set(EMPLOYEE.PASSPORT, dto.getPassport())
                .set(EMPLOYEE.SALARY, dto.getSalary())
                .where(EMPLOYEE.ID.eq(id))
                .returning()
                .fetchOptional()
                .map(record -> record.into(Employee.class))
                .orElseThrow(() -> new RuntimeException("Error updating employee with ID %s with data: %s".formatted(id, dto)));

        log.debug("Employee updated: {}", employee);

        return employee;
    }

    /**
     * Deletes the employee with the specified ID.
     */
    @Delete
    @TablePermissionPoint(id = "EmployeeService.delete", title = "Delete employee", effect = WRITE)
    public void delete(UUID id) {

        log.debug("Deleting the employee with ID {}", id);

        // Delete the record by ID
        dsl.deleteFrom(EMPLOYEE)
                .where(EMPLOYEE.ID.eq(id))
                .execute();

        log.debug("The employee with ID {} has been deleted", id);
    }
}
