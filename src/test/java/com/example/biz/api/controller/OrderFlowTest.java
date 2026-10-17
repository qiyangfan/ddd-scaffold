package com.example.biz.api.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全链路集成测试：H2（MySQL 模式）+ 演示网关，完整走通 api -&gt; application -&gt; domain -&gt; infrastructure。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM t_order_item");
        jdbcTemplate.execute("DELETE FROM t_order");
    }

    @Test
    void createPayAndCancelFlowShouldWork() throws Exception {
        // 1. 创建订单：5999 x 2 = 11998，命中满 500 减 80 档，应付 11918.00
        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1000,"items":[{"productId":2,"quantity":2}],"remark":"deliver before Friday"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("UNPAID"))
                .andExpect(jsonPath("$.data.statusDesc").value("待支付"))
                .andExpect(jsonPath("$.data.totalAmount").value(11998.00))
                .andExpect(jsonPath("$.data.discountAmount").value(80.00))
                .andExpect(jsonPath("$.data.payAmount").value(11918.00))
                .andExpect(jsonPath("$.data.items[0].productName").value("Laptop Pro 14"))
                .andReturn();
        String orderId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.data.id");

        // 2. 支付成功
        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // 3. 重复支付被状态机拒绝
        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0204"));

        // 4. 已支付订单不可取消
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0204"));

        // 5. 分页可查到该订单（total 因 Long 序列化为字符串；主键本身已是 UUID v7 字符串）
        mockMvc.perform(get("/api/v1/orders").param("pageNo", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value("1"))
                .andExpect(jsonPath("$.data.list[0].orderNo").isNotEmpty())
                .andExpect(jsonPath("$.data.list[0].id").isNotEmpty());
    }

    @Test
    void unpaidOrderShouldBeCancellable() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2000,"items":[{"productId":3,"quantity":1}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payAmount").value(99.00))
                .andReturn();
        String orderId = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0204"));
    }

    @Test
    void offSaleProductShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1000,"items":[{"productId":4,"quantity":1}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0212"));
    }

    @Test
    void missingProductShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1000,"items":[{"productId":999,"quantity":1}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0211"));
    }

    @Test
    void invalidParamShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1000,"items":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void missingOrderShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "019b1f2c-7a3e-7c4b-8f1d-2e6a9b0c1d2e"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0201"));
    }

    /** 畸形主键：在进入领域层之前就被转成 400，不带着任意字符串去查库 */
    @Test
    void malformedOrderIdShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    /**
     * 畸形状态枚举：同样 fail-closed，脏过滤值不进 Service、更不下沉到 SQL。
     * 用小写 paid 而非乱码：同时钉住"大小写不宽容"——脏库里可能出现 paid，
     * 它必须被拒，而不是被静默归一成 PAID。
     */
    @Test
    void illegalStatusFilterShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("status", "paid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    /** 分页超限：上限值由领域侧 PageLimits 单一来源强校验 */
    @Test
    void oversizePageShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("pageSize", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0400"));
    }
}
