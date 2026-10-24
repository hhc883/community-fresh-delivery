package com.sky.contracts;

import com.sky.context.BaseContext;
import com.sky.dto.GrouponJoinDTO;
import com.sky.entity.Groupon;
import com.sky.exception.GrouponBusinessException;
import com.sky.properties.JwtProperties;
import com.sky.service.GrouponService;
import com.sky.service.impl.GrouponServiceImpl;
import com.sky.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(GrouponContractTest.ConfigurationUnderTest.class)
@WebAppConfiguration
@Sql("classpath:groupon-contracts-schema.sql")
class GrouponContractTest {
    @Configuration
    @Import({BackendContractTest.TestConfiguration.class, GrouponServiceImpl.class,
            com.sky.controller.user.GrouponController.class})
    static class ConfigurationUnderTest {}

    @Autowired private GrouponService service;
    @Autowired private DataSource dataSource;
    @Autowired private WebApplicationContext context;
    @Autowired private JwtProperties jwt;
    private JdbcTemplate fixtures;
    private MockMvc mvc;

    @BeforeEach void prepare() {
        fixtures = new JdbcTemplate(dataSource);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        BaseContext.removeCurrentId();
        for (String table : new String[]{"groupon_member","groupon","order_detail","orders","dish","address_book","pickup_point","user"}) {
            fixtures.update("DELETE FROM " + table);
        }
        fixtures.update("INSERT INTO dish(id,name,price,status,is_groupon,groupon_price,groupon_size,groupon_valid_hours,stock) VALUES(1,'Apples',12.00,1,1,8.50,2,24,10)");
        fixtures.update("INSERT INTO address_book(id,user_id,consignee,phone,detail) VALUES(11,11,'Alice','13800000011','A'),(22,22,'Bob','13800000022','B'),(33,33,'Carol','13800000033','C')");
        fixtures.update("INSERT INTO pickup_point(id,name,status,contact,phone) VALUES(1,'Store',1,'Merchant','13800000999'),(2,'Closed',0,'Merchant','13800000999')");
        fixtures.update("INSERT INTO user(id,name,phone) VALUES(11,'Alice','13800000011'),(22,'Bob','13800000022'),(33,'Carol','13800000033')");
    }

    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }

    @Test void rejectsNonPositiveQuantityInsteadOfSilentlyBuyingOne() {
        BaseContext.setCurrentId(11L);
        GrouponJoinDTO request = request(11L, 0);
        assertThrows(GrouponBusinessException.class, () -> service.openGroupon(request));
        assertTrue(service.listMine(null).isEmpty());
    }

    @Test void openReturnsTheOwnersPayableOrderAndUsesGroupPrice() throws Exception {
        String body = mvc.perform(post("/user/groupon/open").header("Authorization",token(11))
                        .contentType("application/json").content("{\"dishId\":1,\"number\":2,\"addressBookId\":11}"))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.orderId").isString())
                .andReturn().getResponse().getContentAsString();
        String orderId = com.jayway.jsonpath.JsonPath.read(body,"$.data.orderId");
        mvc.perform(get("/api/orders/"+orderId).header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.amount").value(17.00))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.payStatus").value(0));
    }

    @Test void retriedOpenWithSameRequestKeyReturnsSameOrderAndGroup() throws Exception {
        String request = "{\"dishId\":1,\"number\":2,\"addressBookId\":11,\"clientRequestId\":\"open-retry\"}";
        String first = mvc.perform(post("/user/groupon/open").header("Authorization",token(11))
                        .contentType("application/json").content(request))
                .andExpect(jsonPath("$.code").value(1)).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/user/groupon/open").header("Authorization",token(11))
                        .contentType("application/json").content(request))
                .andExpect(jsonPath("$.code").value(1)).andReturn().getResponse().getContentAsString();
        assertEquals((String)com.jayway.jsonpath.JsonPath.read(first,"$.data.id"),
                com.jayway.jsonpath.JsonPath.read(second,"$.data.id"));
        assertEquals((String)com.jayway.jsonpath.JsonPath.read(first,"$.data.orderId"),
                com.jayway.jsonpath.JsonPath.read(second,"$.data.orderId"));
        mvc.perform(get("/api/orders").header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test void pickupRejectsMixedTargetsDisabledPointAndMissingRecipient() throws Exception {
        for (String payload : new String[]{
                "{\"dishId\":1,\"number\":1,\"addressBookId\":11,\"pickupPointId\":1}",
                "{\"dishId\":1,\"number\":1,\"pickupPointId\":2,\"pickupPersonName\":\"Alice\",\"pickupPersonPhone\":\"13800000011\"}",
                "{\"dishId\":1,\"number\":1,\"pickupPointId\":1}",
                "{\"dishId\":1,\"number\":1,\"deliveryType\":2,\"addressBookId\":11}"
        }) {
            mvc.perform(post("/user/groupon/open").header("Authorization",token(11))
                            .contentType("application/json").content(payload))
                    .andExpect(jsonPath("$.code").value(0));
        }
        BaseContext.setCurrentId(11L);
        assertTrue(service.listMine(null).isEmpty());
    }

    @Test void expiredFullGroupCannotBecomeSuccessful() {
        fixtures.update("INSERT INTO groupon(id,dish_id,leader_user_id,groupon_price,required_size,current_size,status,expire_time,create_time) VALUES(100,1,11,8.50,2,2,1,'2000-01-01 00:00:00','2000-01-01 00:00:00')");
        assertFalse(service.tryComplete(100L));
        assertEquals(1, service.getDetail(100L).getStatus());
    }

    @Test void cancellingOngoingGroupCancelsAllOrdersRefundsPaidAndRestoresStockOnce() throws Exception {
        fixtures.update("UPDATE dish SET groupon_size=3 WHERE id=1");
        BaseContext.setCurrentId(11L);
        Groupon leader = service.openGroupon(request(11,2));
        BaseContext.setCurrentId(22L);
        GrouponJoinDTO joining = request(22,1); joining.setGrouponId(leader.getId());
        Groupon participant = service.joinGroupon(joining);
        mvc.perform(post("/api/pay/"+participant.getOrderId()+"/mock-pay").header("Authorization",token(22)))
                .andExpect(jsonPath("$.code").value(200));
        service.cancelOrderGroup(leader.getId(), "member cancellation");
        service.cancelOrderGroup(leader.getId(), "retry cancellation");
        assertEquals(3, service.getDetail(leader.getId()).getStatus());
        assertEquals(10, service.listGrouponDish(null).get(0).getStock());
        mvc.perform(get("/api/orders/"+leader.getOrderId()).header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.status").value(6)).andExpect(jsonPath("$.data.payStatus").value(0));
        mvc.perform(get("/api/orders/"+participant.getOrderId()).header("Authorization",token(22)))
                .andExpect(jsonPath("$.data.status").value(6)).andExpect(jsonPath("$.data.payStatus").value(2));
    }

    private GrouponJoinDTO request(long userId, int quantity) {
        GrouponJoinDTO dto = new GrouponJoinDTO();
        dto.setDishId(1L); dto.setAddressBookId(userId); dto.setNumber(quantity);
        return dto;
    }

    private String token(long id) {
        return "Bearer " + JwtUtil.createJWT(jwt.getUserSecretKey(), 60000,
                new java.util.HashMap<>(Collections.singletonMap("userId",id)));
    }
}
