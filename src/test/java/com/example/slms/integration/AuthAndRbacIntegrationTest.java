package com.example.slms.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.example.slms.entity.UserAccount;
import com.example.slms.entity.enums.Role;
import com.example.slms.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        userAccountRepository.deleteAll();
    }

    @Test
    void adminCanListUsers() throws Exception {
        createUser("admin", "123456", Role.ADMIN);
        createUser("customer", "123456", Role.CUSTOMER);

        String token = loginAndGetToken("admin", "123456");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotListUsers() throws Exception {
        createUser("admin", "123456", Role.ADMIN);
        createUser("customer", "123456", Role.CUSTOMER);

        String token = loginAndGetToken("customer", "123456");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerAndLoginFlowWorks() throws Exception {
        String registerPayload = """
                {
                  \"username\": \"alice\",
                  \"password\": \"secret123\"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk());
    }

    private void createUser(String username, String rawPassword, Role role) {
        UserAccount user = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        userAccountRepository.save(user);
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
