package com.example.slms.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

import com.example.slms.entity.CustomerOrder;
import com.example.slms.entity.UserAccount;
import com.example.slms.entity.enums.OrderStatus;
import com.example.slms.entity.enums.Role;
import com.example.slms.repository.CustomerOrderRepository;
import com.example.slms.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportBatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        customerOrderRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void generateSalesReportShouldSupportExcelFormat() throws Exception {
        createUser("admin", "123456", Role.ADMIN);
        createUser("alice", "123456", Role.CUSTOMER);

        customerOrderRepository.save(CustomerOrder.builder()
                .orderId("ORD-TEST-001")
                .customerUsername("alice")
                .status(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("99.99"))
                .build());

        String token = loginAndGetToken("admin", "123456");

        mockMvc.perform(get("/api/reports/sales")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("format", "EXCEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType").value("SALES"))
                .andExpect(jsonPath("$.exportFormat").value("EXCEL"))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void batchRetryShouldWorkAfterFailedExecution() throws Exception {
        createUser("admin", "123456", Role.ADMIN);
        String token = loginAndGetToken("admin", "123456");

        String runPayload = """
                {
                  \"jobType\": \"AGGREGATE_SALES_DATA\"
                }
                """;

        mockMvc.perform(post("/api/batch/jobs/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(runPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        String retryPayload = """
                {
                  \"jobType\": \"AGGREGATE_SALES_DATA\",
                  \"retryCount\": 1
                }
                """;

        mockMvc.perform(post("/api/batch/jobs/retry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retryPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETRY_ACCEPTED"));

        mockMvc.perform(get("/api/batch/logs")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("jobType", "AGGREGATE_SALES_DATA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    private void createUser(String username, String rawPassword, Role role) {
        userAccountRepository.save(UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String payload = """
                {
                  \"username\": \"%s\",
                  \"password\": \"%s\"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }
}
