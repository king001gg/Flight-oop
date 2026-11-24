package oop.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后端 API 集成测试：使用独立临时数据目录，不污染 data/。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    private static final Path DATA_DIR =
            Paths.get("target", "test-data-" + UUID.randomUUID());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.data-dir", DATA_DIR::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void loginAndPermissions() throws Exception {
        String admin = login("admin", "admin123");
        String customer = login("zhangwei", "123456");

        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isUnauthorized());

        JsonNode flights = getJson(get("/api/flights"), admin);
        assertEquals(10, flights.path("data").size());

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        JsonNode me = getJson(get("/api/customers/me"), customer);
        assertEquals("张伟", me.path("data").path("name").asText());
    }

    @Test
    @Order(3)
    void queryAndStatistics() throws Exception {
        String admin = login("admin", "admin123");

        MvcResult routeResult = mockMvc.perform(get("/api/flights")
                        .param("from", "桂林")
                        .param("to", "西安")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(3, read(routeResult).path("data").size());

        JsonNode stats = getJson(get("/api/stats/route")
                .param("startCity", "桂林")
                .param("endCity", "西安")
                .param("from", "2026-11-01")
                .param("to", "2026-11-30"), admin);
        assertEquals(3, stats.path("data").path("flightCount").asInt());
        assertEquals(368, stats.path("data").path("totalSeats").asInt());
        assertEquals(12, stats.path("data").path("passengers").asInt());

        JsonNode monthly = getJson(get("/api/stats/monthly"), admin);
        assertEquals(3, monthly.path("data").size());
        assertEquals("2026-09", monthly.path("data").get(0).path("month").asText());
        assertEquals(21, monthly.path("data").get(2).path("netTickets").asInt());
    }

    @Test
    @Order(4)
    void fullFlightReturnsAlternatives() throws Exception {
        String customer = login("zhangwei", "123456");

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightId":"CA1235","quantity":1}
                                """))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode alternatives = read(result).path("data").path("alternatives");
        assertEquals(2, alternatives.size());
        assertEquals("CA1229", alternatives.get(0).path("flightId").asText());
    }

    @Test
    @Order(5)
    void bookAndRefundRestoresSeats() throws Exception {
        String customer = login("zhangwei", "123456");
        int before = getJson(get("/api/flights/CA1234"), customer)
                .path("data").path("remainingSeats").asInt();

        MvcResult booked = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightId":"CA1234","quantity":2}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode order = read(booked).path("data");
        String orderId = order.path("orderId").asText();
        assertTrue(orderId.startsWith("ORD"));
        assertEquals(before - 2, getJson(get("/api/flights/CA1234"), customer)
                .path("data").path("remainingSeats").asInt());

        mockMvc.perform(post("/api/orders/" + orderId + "/refund")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk());

        assertEquals(before, getJson(get("/api/flights/CA1234"), customer)
                .path("data").path("remainingSeats").asInt());
        assertEquals("REFUNDED", getJson(get("/api/orders/" + orderId), customer)
                .path("data").path("status").asText());
    }

    @Test
    @Order(6)
    void customerCannotTouchOthersOrder() throws Exception {
        String customer = login("zhangwei", "123456");
        mockMvc.perform(get("/api/orders/ORD20261110090000-001")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    void adminCrudConstraints() throws Exception {
        String admin = login("admin", "admin123");

        // 有订单的客户不能删除
        mockMvc.perform(delete("/api/admin/customers/450102199001011234")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());

        // 有订单的航班不能删除
        mockMvc.perform(delete("/api/admin/flights/CA1234")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());

        // 客户 CRUD
        String customerJson = """
                {"customerId":"TESTC001","name":"测试客户","phone":"13800000000",
                 "email":"test@example.com","level":"NORMAL"}
                """;
        mockMvc.perform(post("/api/admin/customers")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/customers/TESTC001")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"TESTC001","name":"测试客户改","phone":"13800000001",
                                 "email":"test2@example.com","level":"SILVER"}
                                """))
                .andExpect(status().isOk());

        // 用户 CRUD（客户角色关联 TESTC001）
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"123456","role":"CUSTOMER",
                                 "displayName":"测试用户","customerId":"TESTC001"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/users/testuser")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/customers/TESTC001")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        // 航班 CRUD + 改价
        mockMvc.perform(post("/api/admin/flights")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"flightId":"T9999","date":"2026-12-20","takeOffTime":"09:00",
                                 "durationMinutes":90,"startCity":"桂林","endCity":"广州",
                                 "planeModel":"A320","price":500,"discount":0.8,
                                 "totalSeats":100,"airline":"测试航空","onTimeRate":99}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/flights/T9999/fare")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":520,"discount":0.75}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/flights/T9999")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("token").asText();
    }

    private JsonNode getJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                             String token) throws Exception {
        MvcResult result = mockMvc.perform(builder.header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result);
    }

    private static JsonNode read(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
