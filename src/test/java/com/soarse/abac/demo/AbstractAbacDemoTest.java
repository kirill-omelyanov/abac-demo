package com.soarse.abac.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soarse.abac.demo.model.Employee;
import com.soarse.abac.demo.model.Publication;
import com.soarse.abac.model.action.ActionEffect;
import com.soarse.abac.model.policy.Policy;
import com.soarse.abac.service.PolicyService;
import com.soarse.abac.service.PolicySource;
import com.soarse.common.model.data.entity.id.TableEntityId;
import com.soarse.formula.Formula;
import com.soarse.formula.context.source.UserAttributeSource;
import com.soarse.test.AbstractPostgresIntegrationTest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.soarse.common.util.file.ClassPathFileLoader.read;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.util.Lists.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
public abstract class AbstractAbacDemoTest extends AbstractPostgresIntegrationTest {

    protected static final String ADMIN = "/user/admin.json";
    protected static final String CHIEF_EDITOR = "/user/chiefeditor.json";
    protected static final String ACCOUNTANT = "/user/accountant.json";
    protected static final String JOURNALIST = "/user/journalist.json";

    protected static final String NY_BRANCH = "NY";
    protected static final String LA_BRANCH = "LA";

    protected static final String SERVICE = "abac-demo";
    protected static final String SCHEMA = "abac_demo";

    protected static final TableEntityId EMPLOYEE_TABLE_ID = new TableEntityId(SERVICE, SCHEMA, "employee");
    protected static final TableEntityId PUBLICATION_TABLE_ID = new TableEntityId(SERVICE, SCHEMA, "publication");

    private static final TypeReference<HashMap<String, Formula>> FORMULA_MAP_TYPE_REFERENCE = new TypeReference<>() {
    };


    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected PolicyService policyService;

    @MockBean
    protected PolicySource policySource;

    @MockBean
    protected UserAttributeSource userAttributeSource;

    @BeforeEach
    public void init() {

        resetPolicies();
    }

    protected void setUser(String path) {

        var userJson = readJsonObject(path);
        when(userAttributeSource.getUserAttributes()).thenReturn(userJson);
    }

    protected void resetPolicies() {

        when(policySource.getUniversalPolicies())
                .thenReturn(emptyList());

        when(policySource.getActionPolicies(anyString()))
                .thenReturn(emptyList());

        when(policySource.getEntityPolicies(any(), any()))
                .thenReturn(emptyList());

        policyService.evictCache();
    }

    protected void setUniversalPolicies(String... paths) {

        when(policySource.getUniversalPolicies())
                .thenReturn(readPolicies(paths));
    }

    protected void setActionPolicies(String actionId, String... paths) {

        when(policySource.getActionPolicies(actionId))
                .thenReturn(readPolicies(paths));
    }

    protected void setTableEntityPolicies(ActionEffect effect, TableEntityId entityId, String... paths) {

        when(policySource.getEntityPolicies(effect, entityId))
                .thenReturn(readPolicies(paths));
    }

    @NotNull
    private List<Policy> readPolicies(String[] paths) {

        return stream(paths)
                .map(this::readPolicy)
                .toList();
    }

    @SneakyThrows
    protected String readDocument(String path) {

        return read(path, AbstractPostgresIntegrationTest.class);
    }

    @SneakyThrows
    protected JsonNode readJson(String path) {

        return mapper.readTree(readDocument(path));
    }

    @SneakyThrows
    protected ObjectNode readJsonObject(String path) {

        return (ObjectNode) readJson(path);
    }

    @SneakyThrows
    protected Policy readPolicy(String path) {

        return mapper.readValue(readDocument(path), Policy.class);
    }

    @SneakyThrows
    protected Formula readFormula(String path) {

        return mapper.readValue(readDocument(path), Formula.class);
    }

    @SneakyThrows
    protected Map<String, Formula> readComputationalPropertyFormulas(String path) {

        return mapper.readValue(readDocument(path), FORMULA_MAP_TYPE_REFERENCE);
    }

    protected void logEmployees(Collection<Employee> employees) {

        // Log the list of entries line by line
        log.debug("Employees:\n{}", employees.stream().map(Employee::toString).collect(joining(",\n")));
    }

    protected void logPublications(Collection<Publication> publications) {

        // Log the list of entries line by line
        log.debug("Publications:\n{}", publications.stream().map(Publication::toString).collect(joining(",\n")));
    }
}
