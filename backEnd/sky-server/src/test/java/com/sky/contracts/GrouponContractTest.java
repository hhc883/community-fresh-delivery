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
            com.sky.controller.user.GrouponController.class, com.sky.service.impl.ReportServiceImpl.class,
            com.sky.service.impl.WorkspaceServiceImpl.class})
    static class ConfigurationUnderTest {}

    @Autowired private GrouponService service;
    @Autowired private com.sky.service.ReportService reports;
    @Autowired private DataSource dataSource;
    @Autowired private WebApplicationContext context;
    @Autowired private JwtProperties jwt;
    private JdbcTemplate fixtures;
    private MockMvc mvc;

    @BeforeEach void prepare() {
        fixtures = new JdbcTemplate(dataSource);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        BaseContext.removeCurrentId();
        for (String table : new String[]{"order_stock","groupon_member","groupon","order_detail","orders","dish","address_book","pickup_point","user"}) {
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

    @Test void participantOperationsRequireAuthenticationAtBothBoundaries() throws Exception {
        assertThrows(GrouponBusinessException.class, () -> service.listMine(null));
        assertThrows(GrouponBusinessException.class, () -> service.openGroupon(request(11,1)));
        mvc.perform(post("/user/groupon/open").contentType("application/json")
                        .content("{\"dishId\":1,\"number\":1,\"addressBookId\":11}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/user/groupon/mine")).andExpect(status().isUnauthorized());
    }

    @Test void cancellingUnmanagedStockDoesNotInventAnInventoryReservation() {
        fixtures.update("UPDATE dish SET stock=NULL WHERE id=1");
        BaseContext.setCurrentId(11L);
        Groupon group = service.openGroupon(request(11,2));
        fixtures.update("UPDATE dish SET stock=50 WHERE id=1");
        service.cancelOrderGroup(group.getId(), "cancel");
        assertEquals(50, service.listGrouponDish(null).get(0).getStock());
    }

    @Test void turnoverReportIncludesEveryDayAndOnlyCompletedPaidSales() {
        fixtures.update("INSERT INTO orders(number,status,user_id,pay_status,amount,order_time) VALUES('sale-1',5,11,1,17.00,'2026-10-22 00:00:00'),('sale-2',5,11,1,8.50,'2026-10-23 12:00:00'),('refund',6,11,2,50.00,'2026-10-22 12:00:00'),('unpaid',5,11,0,999.00,'2026-10-23 12:00:00')");
        com.sky.vo.TurnoverReportVO report = reports.getTurnoverReport(java.time.LocalDate.of(2026,10,22),java.time.LocalDate.of(2026,10,23));
        assertEquals("2026-10-22,2026-10-23",report.getDateList());
        assertEquals("17.0,8.5",report.getTurnoverList());
    }

    @Test void groupStatisticsUsesLiteralActivityCountsAndExcludesCancelledReceipts() {
        fixtures.update("INSERT INTO groupon(id,dish_id,leader_user_id,status,create_time) VALUES(101,1,11,1,'2026-10-22 00:00:00'),(102,1,11,2,'2026-10-22 12:00:00'),(103,1,11,3,'2026-10-22 12:00:00'),(104,1,11,4,'2026-10-22 12:00:00')");
        fixtures.update("INSERT INTO orders(id,number,status,user_id,pay_status,is_groupon,groupon_id,amount,order_time) VALUES(101,'group-paid',2,11,1,1,101,17.00,'2026-10-22 00:00:00'),(102,'group-done',5,11,1,1,102,8.50,'2026-10-22 12:00:00'),(103,'group-cancelled',6,11,1,1,103,50.00,'2026-10-22 12:00:00'),(104,'group-refund',6,11,2,1,104,80.00,'2026-10-22 12:00:00')");
        fixtures.update("INSERT INTO groupon_member(groupon_id,user_id,order_id) VALUES(101,11,101),(102,11,102),(103,11,103),(104,11,104)");
        com.sky.vo.GrouponStatisticsVO report = reports.getGrouponStatistics(java.time.LocalDate.of(2026,10,22),java.time.LocalDate.of(2026,10,22));
        assertEquals(1,report.getOngoing()); assertEquals(1,report.getSuccess());
        assertEquals(1,report.getFailed()); assertEquals(1,report.getExpired());
        assertEquals(4,report.getMemberCount()); assertEquals(4,report.getGrouponOrderCount());
        assertEquals(new java.math.BigDecimal("25.50"),report.getGrouponTurnover());
    }

    @Test void detailAndMineExposeOnlyTheCallersOrderAndPickupSnapshotsTheRecipient() throws Exception {
        String body = mvc.perform(post("/user/groupon/open").header("Authorization",token(11))
                        .contentType("application/json").content("{\"dishId\":1,\"number\":1,\"pickupPointId\":1,\"deliveryType\":2,\"pickupPersonName\":\"Alice\",\"pickupPersonPhone\":\"13800000011\"}"))
                .andExpect(jsonPath("$.code").value(1)).andReturn().getResponse().getContentAsString();
        String orderId = com.jayway.jsonpath.JsonPath.read(body,"$.data.orderId");
        String groupId = com.jayway.jsonpath.JsonPath.read(body,"$.data.id");
        mvc.perform(get("/user/groupon/detail/"+groupId).header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.members[0].orderId").doesNotExist())
                .andExpect(jsonPath("$.data.members[0].phone").doesNotExist());
        mvc.perform(get("/user/groupon/detail/"+groupId).header("Authorization",token(22)))
                .andExpect(jsonPath("$.data.orderId").isEmpty());
        mvc.perform(get("/user/groupon/mine").header("Authorization",token(11)))
                .andExpect(jsonPath("$.data[0].orderId").value(orderId));
        mvc.perform(get("/user/groupon/mine").header("Authorization",token(22)))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/orders/"+orderId).header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.pickupPersonName").value("Alice"))
                .andExpect(jsonPath("$.data.pickupPersonPhone").value("13800000011"))
                .andExpect(jsonPath("$.data.pickupPointName").value("Store"));
    }

    @Test void simultaneousExpiryClosesAGroupOnlyOnce() throws Exception {
        BaseContext.setCurrentId(11L);
        Groupon group = service.openGroupon(request(11,2));
        fixtures.update("UPDATE groupon SET expire_time='2000-01-01 00:00:00' WHERE id=?",group.getId());
        java.util.List<Integer> closed = runBoth(service::closeExpired, service::closeExpired);
        assertEquals(1,closed.get(0)+closed.get(1));
        assertEquals(10,service.listGrouponDish(null).get(0).getStock());
        assertEquals(4,service.getDetail(group.getId()).getStatus());
    }

    @Test void eachExpiredGroupCommitsIndependentlyOfALaterInvalidLegacyOrder() {
        fixtures.update("INSERT INTO dish(id,name,price,status,is_groupon,groupon_price,groupon_size,groupon_valid_hours,stock) VALUES(2,'Pears',6.00,1,1,5.00,2,24,10)");
        BaseContext.setCurrentId(11L);
        Groupon first = service.openGroupon(request(11,1));
        BaseContext.setCurrentId(22L);
        GrouponJoinDTO secondRequest = request(22,1); secondRequest.setDishId(2L);
        Groupon second = service.openGroupon(secondRequest);
        fixtures.update("UPDATE groupon SET expire_time='2000-01-01 00:00:00'");
        fixtures.update("UPDATE orders SET status=3,pay_status=1 WHERE id=?",second.getOrderId());
        assertThrows(GrouponBusinessException.class, service::closeExpired);
        assertEquals(4,service.getDetail(first.getId()).getStatus());
        assertEquals(1,service.getDetail(second.getId()).getStatus());
        assertEquals(10,service.listGrouponDish(null).stream().filter(d -> d.getId().equals(1L)).findFirst().get().getStock());
        assertEquals(9,service.listGrouponDish(null).stream().filter(d -> d.getId().equals(2L)).findFirst().get().getStock());
    }

    @Test void emptyAndSingleDayReportsHaveDefinedResultsAndReversedDatesAreRejected() {
        java.time.LocalDate day = java.time.LocalDate.of(2026,10,22);
        assertEquals("2026-10-22",reports.getTurnoverReport(day,day).getDateList());
        assertEquals("0.0",reports.getTurnoverReport(day,day).getTurnoverList());
        com.sky.vo.GrouponStatisticsVO empty = reports.getGrouponStatistics(null,null);
        assertEquals(0,empty.getOngoing()); assertEquals(0,empty.getSuccess());
        assertEquals(0,empty.getFailed()); assertEquals(0,empty.getExpired());
        assertEquals(0,empty.getMemberCount()); assertEquals(0,empty.getGrouponOrderCount());
        assertEquals(java.math.BigDecimal.ZERO,empty.getGrouponTurnover().stripTrailingZeros());
        assertThrows(com.sky.exception.BaseException.class, () -> reports.getTurnoverReport(day,day.minusDays(1)));
        assertThrows(com.sky.exception.BaseException.class, () -> reports.getOrderStatistics(day,day.minusDays(1)));
        assertThrows(com.sky.exception.BaseException.class, () -> reports.getUserStatistics(day,day.minusDays(1)));
        assertThrows(com.sky.exception.BaseException.class, () -> reports.getSalesTop10(day,day.minusDays(1)));
        assertThrows(com.sky.exception.BaseException.class, () -> reports.getGrouponStatistics(day,day.minusDays(1)));
    }

    @Test void onlyOneContenderGetsLastPlaceAndCompletionDoesNotPayOrders() throws Exception {
        BaseContext.setCurrentId(11L);
        Groupon group = service.openGroupon(request(11,1));
        java.util.List<Long> results = runBoth(() -> joinAs(22, group.getId()), () -> joinAs(33, group.getId()));
        assertEquals(1, results.stream().filter(java.util.Objects::nonNull).count());
        assertEquals(2, service.getDetail(group.getId()).getCurrentSize());
        assertEquals(2, service.getDetail(group.getId()).getStatus());
        assertEquals(2, service.getDetail(group.getId()).getMembers().size());
        assertEquals(8, service.listGrouponDish(null).get(0).getStock());
        assertThrows(GrouponBusinessException.class, () -> service.cancelOrderGroup(group.getId(),"too late"));
        mvc.perform(get("/api/orders/"+group.getOrderId()).header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.status").value(1)).andExpect(jsonPath("$.data.payStatus").value(0));
        int winner = results.get(0) != null ? 0 : 1;
        mvc.perform(get("/api/orders/"+results.get(winner)).header("Authorization",token(winner == 0 ? 22 : 33)))
                .andExpect(jsonPath("$.data.status").value(1)).andExpect(jsonPath("$.data.payStatus").value(0));
    }

    @Test void duplicateConcurrentJoinIsDeterministicallyRejected() throws Exception {
        fixtures.update("UPDATE dish SET groupon_size=3 WHERE id=1");
        BaseContext.setCurrentId(11L);
        Groupon group = service.openGroupon(request(11,1));
        java.util.List<Long> results = runBoth(() -> joinAs(22,group.getId()), () -> joinAs(22,group.getId()));
        assertEquals(1, results.stream().filter(java.util.Objects::nonNull).count());
        assertEquals(2, service.getDetail(group.getId()).getCurrentSize());
        assertEquals(2, service.getDetail(group.getId()).getMembers().size());
        assertEquals(8, service.listGrouponDish(null).get(0).getStock());
    }

    @Test void expiryWinsAgainstLateJoinAndRetryDoesNotRefundOrRestoreTwice() throws Exception {
        BaseContext.setCurrentId(11L);
        Groupon group = service.openGroupon(request(11,2));
        mvc.perform(post("/api/pay/"+group.getOrderId()+"/mock-pay").header("Authorization",token(11)))
                .andExpect(jsonPath("$.code").value(200));
        fixtures.update("UPDATE groupon SET expire_time='2000-01-01 00:00:00' WHERE id=?",group.getId());
        java.util.List<Integer> results = runBoth(service::closeExpired, () -> joinAs(22,group.getId()) == null ? 0 : 100);
        assertEquals(java.util.Arrays.asList(1,0), results);
        assertEquals(0, service.closeExpired());
        assertEquals(4, service.getDetail(group.getId()).getStatus());
        assertEquals(1, service.getDetail(group.getId()).getMembers().size());
        assertEquals(10, service.listGrouponDish(null).get(0).getStock());
        mvc.perform(get("/api/orders/"+group.getOrderId()).header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.status").value(6)).andExpect(jsonPath("$.data.payStatus").value(2));
    }

    private Long joinAs(long userId, Long groupId) {
        BaseContext.setCurrentId(userId);
        GrouponJoinDTO dto = request(userId,1); dto.setGrouponId(groupId);
        try { return service.joinGroupon(dto).getOrderId(); }
        catch (GrouponBusinessException expectedRejection) { return null; }
        finally { BaseContext.removeCurrentId(); }
    }

    private <T> java.util.List<T> runBoth(java.util.concurrent.Callable<T> first, java.util.concurrent.Callable<T> second) throws Exception {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<T>> futures = new java.util.ArrayList<>();
        try {
            for (java.util.concurrent.Callable<T> action : java.util.Arrays.asList(first,second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("start barrier timed out");
                    try { return action.call(); } finally { BaseContext.removeCurrentId(); }
                }));
            }
            assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            return java.util.Arrays.asList(futures.get(0).get(20,java.util.concurrent.TimeUnit.SECONDS), futures.get(1).get(20,java.util.concurrent.TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
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
