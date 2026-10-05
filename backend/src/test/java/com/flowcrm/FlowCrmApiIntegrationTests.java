package com.flowcrm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.auth.AuthService;
import com.flowcrm.auth.dto.AuthResponse;
import com.flowcrm.auth.dto.LoginRequest;
import com.flowcrm.auth.dto.RefreshRequest;
import com.flowcrm.auth.dto.RegisterRequest;
import com.flowcrm.client.ClientRepository;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.opportunity.OpportunityRepository;
import com.flowcrm.shared.security.JwtService;
import com.flowcrm.shared.security.RateLimitCounterStore;
import com.flowcrm.shared.security.SecurityDataRetentionJob;
import com.flowcrm.task.TaskRepository;
import com.flowcrm.user.UserRepository;
import com.flowcrm.user.Role;
import com.flowcrm.user.UserService;
import com.flowcrm.user.dto.UserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FlowCrmApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitCounterStore rateLimitCounterStore;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SecurityDataRetentionJob securityDataRetentionJob;

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health").secure(true))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Frame-Options", "DENY"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .exists("Strict-Transport-Security"));
    }

    @Test
    void prometheusMetricsEndpointIsAvailableForInternalScraping() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void crmEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/leads"))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        Instant requestStartedAt = Instant.now();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{email:malformed}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"));

        Integer auditEvents = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM audit_events
                        WHERE http_method = 'POST'
                          AND request_path = '/api/v1/auth/login'
                          AND response_status = 400
                          AND occurred_at >= ?
                        """,
                Integer.class,
                Timestamp.from(requestStartedAt)
        );
        assertEquals(1, auditEvents);
    }

    @Test
    void rejectedRateLimitedLoginIsStoredInAuditLog() throws Exception {
        String remoteAddress = "203.0.113." + UUID.randomUUID().toString().substring(0, 8);
        Instant requestStartedAt = Instant.now();

        for (int attempt = 0; attempt < 11; attempt++) {
            var request = post("/api/v1/auth/login")
                    .with(servletRequest -> {
                        servletRequest.setRemoteAddr(remoteAddress);
                        return servletRequest;
                    })
                    .contentType("application/json")
                    .content("{\"email\":\"unknown@example.com\",\"password\":\"wrong-password\"}");
            var result = mockMvc.perform(request);
            if (attempt < 10) {
                result.andExpect(status().isUnauthorized());
            } else {
                result.andExpect(status().is(429));
            }
        }

        Integer rejectedAttempts = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM audit_events
                        WHERE http_method = 'POST'
                          AND request_path = '/api/v1/auth/login'
                          AND response_status = 429
                          AND remote_addr = ?
                          AND occurred_at >= ?
                        """,
                Integer.class,
                remoteAddress,
                Timestamp.from(requestStartedAt)
        );
        assertEquals(1, rejectedAttempts);
    }

    @Test
    void rateLimitCountersAreSharedThroughPostgres() {
        String remoteAddress = UUID.randomUUID().toString();
        String endpoint = "/api/v1/auth/login";

        assertEquals(1, rateLimitCounterStore.consume(remoteAddress, endpoint, 2, 60).attempts());
        assertEquals(2, rateLimitCounterStore.consume(remoteAddress, endpoint, 2, 60).attempts());
        assertEquals(3, rateLimitCounterStore.consume(remoteAddress, endpoint, 2, 60).attempts());
        assertEquals(1, rateLimitCounterStore.consume(remoteAddress, "/api/v1/auth/register", 2, 60).attempts());
    }

    @Test
    void rateLimitCounterStoreCountsConcurrentRequestsAtomically() {
        String remoteAddress = UUID.randomUUID().toString();

        List<RateLimitCounterStore.RateLimitDecision> decisions = IntStream.range(0, 16)
                .parallel()
                .mapToObj(ignored -> rateLimitCounterStore.consume(
                        remoteAddress,
                        "/api/v1/auth/login",
                        5,
                        60
                ))
                .toList();

        assertEquals(5, decisions.stream().filter(decision -> decision.attempts() <= 5).count());
        assertEquals(6, decisions.stream().mapToInt(RateLimitCounterStore.RateLimitDecision::attempts).max().orElse(0));
    }

    @Test
    void securityRetentionRemovesExpiredCountersAndAuditEvents() {
        String counterKey = UUID.randomUUID().toString().replace("-", "").repeat(2);
        String remoteAddress = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                        INSERT INTO auth_rate_limit_counters
                            (counter_key, window_started_at, attempts, updated_at)
                        VALUES (?, clock_timestamp() - INTERVAL '2 days', 1, clock_timestamp() - INTERVAL '2 days')
                        """,
                counterKey
        );
        jdbcTemplate.update(
                """
                        INSERT INTO audit_events
                            (http_method, request_path, response_status, remote_addr, occurred_at)
                        VALUES ('POST', '/api/v1/auth/login', 401, ?, clock_timestamp() - INTERVAL '91 days')
                        """,
                remoteAddress
        );

        securityDataRetentionJob.cleanExpiredSecurityData();

        Integer counters = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_rate_limit_counters WHERE counter_key = ?",
                Integer.class,
                counterKey
        );
        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE remote_addr = ?",
                Integer.class,
                remoteAddress
        );
        assertEquals(0, counters);
        assertEquals(0, events);
    }

    @Test
    void salesRoleCannotListUsers() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "sales-permission-" + suffix + "@example.com";
        AuthResponse registered = authService.register(new RegisterRequest(
                "Permission integration test",
                "permission-test-" + suffix,
                "Sales permission test",
                email,
                "temporary-test-password"
        ));

        try {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + registered.accessToken()))
                    .andExpect(status().isForbidden());
        } finally {
            userRepository.deleteById(registered.userId());
            companyRepository.deleteById(registered.companyId());
        }
    }

    @Test
    void clientRecordsAreIsolatedBetweenCompanies() throws Exception {
        String firstSuffix = UUID.randomUUID().toString().replace("-", "");
        String secondSuffix = UUID.randomUUID().toString().replace("-", "");
        AuthResponse firstTenant = authService.register(new RegisterRequest(
                "Client isolation first company",
                "client-isolation-first-" + firstSuffix,
                "First tenant admin",
                "client-first-" + firstSuffix + "@example.com",
                "temporary-test-password"
        ));
        AuthResponse secondTenant = authService.register(new RegisterRequest(
                "Client isolation second company",
                "client-isolation-second-" + secondSuffix,
                "Second tenant admin",
                "client-second-" + secondSuffix + "@example.com",
                "temporary-test-password"
        ));
        UUID clientId = null;
        UUID opportunityId = null;

        try {
            String response = mockMvc.perform(post("/api/v1/clients")
                            .header("Authorization", "Bearer " + firstTenant.accessToken())
                            .contentType("application/json")
                            .content("{\"name\":\"Tenant scoped client\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Tenant scoped client"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            clientId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

            String opportunityResponse = mockMvc.perform(post("/api/v1/opportunities")
                            .header("Authorization", "Bearer " + firstTenant.accessToken())
                            .contentType("application/json")
                            .content("""
                                    {"title":"Tenant scoped opportunity","value":125.50,"stage":"NEW",
                                     "probability":10,"clientId":"%s"}
                                    """.formatted(clientId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientName").value("Tenant scoped client"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            opportunityId = UUID.fromString(objectMapper.readTree(opportunityResponse).get("id").asText());

            mockMvc.perform(get("/api/v1/clients?size=100")
                            .header("Authorization", "Bearer " + firstTenant.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Tenant scoped client"));

            mockMvc.perform(get("/api/v1/clients?size=100")
                            .header("Authorization", "Bearer " + secondTenant.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));

            mockMvc.perform(get("/api/v1/opportunities?size=100")
                            .header("Authorization", "Bearer " + firstTenant.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].clientName").value("Tenant scoped client"));

            mockMvc.perform(get("/api/v1/opportunities?size=100")
                            .header("Authorization", "Bearer " + secondTenant.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        } finally {
            if (opportunityId != null) {
                opportunityRepository.deleteById(opportunityId);
            }
            if (clientId != null) {
                clientRepository.deleteById(clientId);
            }
            userRepository.deleteById(firstTenant.userId());
            companyRepository.deleteById(firstTenant.companyId());
            userRepository.deleteById(secondTenant.userId());
            companyRepository.deleteById(secondTenant.companyId());
        }
    }

    @Test
    void taskCanBeCreatedAndCompletedThroughTheApi() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        AuthResponse registered = authService.register(new RegisterRequest(
                "Task integration test",
                "task-test-" + suffix,
                "Task test admin",
                "task-test-" + suffix + "@example.com",
                "temporary-test-password"
        ));
        UUID taskId = null;

        try {
            String dueDate = Instant.now().minusSeconds(3600).toString();
            String response = mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", "Bearer " + registered.accessToken())
                            .contentType("application/json")
                            .content("""
                                    {"title":"Integration task","status":"TODO","priority":"HIGH",
                                     "dueDate":"%s","assignedToId":"%s"}
                                    """.formatted(dueDate, registered.userId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Integration task"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            taskId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

            mockMvc.perform(get("/api/v1/tasks?search=Integration%20task")
                            .header("Authorization", "Bearer " + registered.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].assignedToName").value("Task test admin"));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .patch("/api/v1/tasks/" + taskId + "/complete")
                            .header("Authorization", "Bearer " + registered.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            mockMvc.perform(get("/api/v1/tasks/" + taskId)
                            .header("Authorization", "Bearer " + registered.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedToName").value("Task test admin"));
        } finally {
            if (taskId != null) {
                taskRepository.deleteById(taskId);
            }
            userRepository.deleteById(registered.userId());
            companyRepository.deleteById(registered.companyId());
        }
    }

    @Test
    void salesCanOnlyCreateAndCompleteOwnTasks() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        AuthResponse registered = authService.register(new RegisterRequest(
                "Task authorization test",
                "task-auth-test-" + suffix,
                "Task authorization admin",
                "task-auth-test-" + suffix + "@example.com",
                "temporary-test-password"
        ));
        String salesToken = jwtService.generateAccessToken(
                registered.userId(),
                registered.companyId(),
                registered.email(),
                "SALES"
        );
        UUID ownTaskId = null;
        UUID unassignedTaskId = null;
        UUID otherTaskId = null;
        UUID otherUserId = null;

        try {
            otherUserId = userService.create(new UserRequest(
                    registered.companyId(),
                    "Task coworker",
                    "task-coworker-" + suffix + "@example.com",
                    "temporary-test-password",
                    Role.USER,
                    true
            )).id();

            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", "Bearer " + salesToken)
                            .contentType("application/json")
                            .content("""
                                    {"title":"Forbidden assigned task","assignedToId":"%s"}
                                    """.formatted(otherUserId)))
                    .andExpect(status().isForbidden());

            String otherTask = mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", "Bearer " + registered.accessToken())
                            .contentType("application/json")
                            .content("""
                                    {"title":"Coworker task","assignedToId":"%s"}
                                    """.formatted(otherUserId)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            otherTaskId = UUID.fromString(objectMapper.readTree(otherTask).get("id").asText());

            String ownTask = mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", "Bearer " + salesToken)
                            .contentType("application/json")
                            .content("""
                                    {"title":"Own task","assignedToId":"%s"}
                                    """.formatted(registered.userId())))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            ownTaskId = UUID.fromString(objectMapper.readTree(ownTask).get("id").asText());

            String unassignedTask = mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", "Bearer " + salesToken)
                            .contentType("application/json")
                            .content("{\"title\":\"Unassigned task\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            unassignedTaskId = UUID.fromString(objectMapper.readTree(unassignedTask).get("id").asText());
            assertEquals(
                    registered.userId().toString(),
                    objectMapper.readTree(unassignedTask).get("assignedToId").asText()
            );

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .put("/api/v1/tasks/" + otherTaskId)
                            .header("Authorization", "Bearer " + salesToken)
                            .contentType("application/json")
                            .content("""
                                    {"title":"Claim another task","assignedToId":"%s"}
                                    """.formatted(registered.userId())))
                    .andExpect(status().isForbidden());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .patch("/api/v1/tasks/" + ownTaskId + "/complete")
                            .header("Authorization", "Bearer " + salesToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedToId").value(registered.userId().toString()));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .patch("/api/v1/tasks/" + unassignedTaskId + "/complete")
                            .header("Authorization", "Bearer " + salesToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .patch("/api/v1/tasks/" + otherTaskId + "/complete")
                            .header("Authorization", "Bearer " + salesToken))
                    .andExpect(status().isForbidden());
        } finally {
            if (ownTaskId != null) {
                taskRepository.deleteById(ownTaskId);
            }
            if (unassignedTaskId != null) {
                taskRepository.deleteById(unassignedTaskId);
            }
            if (otherTaskId != null) {
                taskRepository.deleteById(otherTaskId);
            }
            if (otherUserId != null) {
                userRepository.deleteById(otherUserId);
            }
            userRepository.deleteById(registered.userId());
            companyRepository.deleteById(registered.companyId());
        }
    }

    @Test
    void authenticationCanLoadUserAndCompanyAfterRegistrationTransaction() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "auth-test-" + suffix + "@example.com";
        AuthResponse registered = authService.register(new RegisterRequest(
                "Authentication integration test",
                "auth-test-" + suffix,
                "Authentication test",
                email,
                "temporary-test-password"
        ));

        try {
            assertEquals(
                    registered.userId(),
                    authService.login(new LoginRequest(email, "temporary-test-password")).userId()
            );
            assertEquals(
                    registered.userId(),
                    authService.refresh(new RefreshRequest(registered.refreshToken())).userId()
            );
            assertEquals(email, authService.getMe(registered.userId(), registered.companyId()).email());
        } finally {
            userRepository.deleteById(registered.userId());
            companyRepository.deleteById(registered.companyId());
        }
    }
}
