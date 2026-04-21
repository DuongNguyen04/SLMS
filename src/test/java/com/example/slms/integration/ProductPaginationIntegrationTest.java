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

import com.example.slms.entity.Product;
import com.example.slms.entity.UserAccount;
import com.example.slms.entity.enums.Role;
import com.example.slms.repository.ProductRepository;
import com.example.slms.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        userAccountRepository.deleteAll();

        createUser("customer", "123456", Role.CUSTOMER);
        productRepository.save(Product.builder().name("Wireless Mouse").price(new BigDecimal("29.99")).stockQuantity(10).build());
        productRepository.save(Product.builder().name("Gaming Keyboard").price(new BigDecimal("79.99")).stockQuantity(8).build());
        productRepository.save(Product.builder().name("Mouse Pad").price(new BigDecimal("9.99")).stockQuantity(25).build());
    }

    @Test
    void listProductsSupportsPaginationAndFiltering() throws Exception {
        String token = loginAndGetToken("customer", "123456");

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .queryParam("keyword", "mouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").exists())
                .andExpect(jsonPath("$.content[0].stockQuantity").exists());

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("minPrice", "10")
                        .queryParam("maxPrice", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
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
