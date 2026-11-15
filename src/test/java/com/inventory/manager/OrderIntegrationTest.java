package com.inventory.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.manager.dto.request.*;
import com.inventory.manager.dto.response.*;
import com.inventory.manager.entity.ProductStatus;
import com.inventory.manager.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setup() throws Exception {
        RegisterRequest adminReq = new RegisterRequest("Admin User", "admin@store.com", "adminpass", Role.ADMIN);
        MvcResult adminRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated()).andReturn();
        this.adminToken = "Bearer " + objectMapper.readValue(adminRes.getResponse().getContentAsString(), AuthResponse.class).getToken();

        RegisterRequest userReq = new RegisterRequest("Customer One", "customer@store.com", "customerpass", Role.USER);
        MvcResult userRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated()).andReturn();
        this.userToken = "Bearer " + objectMapper.readValue(userRes.getResponse().getContentAsString(), AuthResponse.class).getToken();
    }

    @Test
    void testInventoryAndOrderLifecycle() throws Exception {
        // 1. Admin creates category
        CategoryRequest catReq = new CategoryRequest("Electronics", "Gadgets and devices");
        MvcResult catRes = mockMvc.perform(post("/api/categories")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated()).andReturn();
        Long catId = objectMapper.readValue(catRes.getResponse().getContentAsString(), CategoryResponse.class).getId();

        // 2. Admin creates product with 10 units
        ProductRequest prodReq = new ProductRequest("Wireless Mouse", "Optical mouse", new BigDecimal("25.00"), 10, ProductStatus.ACTIVE, Set.of(catId));
        MvcResult prodRes = mockMvc.perform(post("/api/products")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated()).andReturn();
        Long prodId = objectMapper.readValue(prodRes.getResponse().getContentAsString(), ProductResponse.class).getId();

        // 3. User tries to order 15 units (exceeds stock) -> must fail
        OrderRequest excessReq = new OrderRequest(Collections.singletonList(new OrderItemRequest(prodId, 15)));
        mockMvc.perform(post("/api/orders")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(excessReq)))
                .andExpect(status().isBadRequest());

        // 4. User orders 4 units -> succeeds, stock reduced to 6
        OrderRequest validReq = new OrderRequest(Collections.singletonList(new OrderItemRequest(prodId, 4)));
        MvcResult orderRes = mockMvc.perform(post("/api/orders")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED")).andReturn();
        Long orderId = objectMapper.readValue(orderRes.getResponse().getContentAsString(), OrderResponse.class).getId();

        // Verify product stock is now 6
        mockMvc.perform(get("/api/products/" + prodId)
                .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(6));

        // 5. User cancels order -> stock restored to 10
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify product stock is restored to 10
        mockMvc.perform(get("/api/products/" + prodId)
                .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }
}
