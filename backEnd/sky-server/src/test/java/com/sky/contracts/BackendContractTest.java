package com.sky.contracts;

import com.github.pagehelper.PageInterceptor;
import com.sky.config.WebMvcConfiguration;
import com.sky.context.BaseContext;
import com.sky.controller.api.ApiAuthController;
import com.sky.controller.api.ApiOrderController;
import com.sky.controller.api.ApiPayController;
import com.sky.controller.user.AddressBookController;
import com.sky.handler.GlobalExceptionHandler;
import com.sky.handler.ResultResponseAdvice;
import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.properties.JwtProperties;
import com.sky.service.impl.AddressBookServiceImpl;
import com.sky.service.impl.OrderServiceImpl;
import com.sky.utils.JwtUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real HTTP handlers, authentication, services and MyBatis against an isolated H2 database. */
@SpringJUnitConfig(BackendContractTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {"sky.auth.mock-login=true", "sky.auth.dev-code=123456"})
class BackendContractTest {
    @Autowired private WebApplicationContext context;
    @Autowired private DataSource dataSource;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private com.sky.service.GrouponService groups;
    @Autowired private com.sky.service.OrderService orderService;
    private MockMvc mvc;
    private JdbcTemplate fixtures;

    @BeforeEach
    void setUp() {
        BaseContext.removeCurrentId();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        fixtures = new JdbcTemplate(dataSource);
        for (String table : new String[]{"groupon_member", "groupon", "order_stock", "order_detail", "orders", "shopping_cart", "address_book", "pickup_point", "user", "dish", "setmeal", "setmeal_dish"}) {
            fixtures.update("DELETE FROM " + table);
        }
    }

    @AfterEach
    void clearContext() { BaseContext.removeCurrentId(); }

    @Test
    void directCheckoutUsesCatalogPriceAndNamesInsteadOfClientValues() throws Exception {
        fixtures.update("INSERT INTO user (id,phone) VALUES (11,'13800000011')");
        fixtures.update("INSERT INTO pickup_point (id,name,address,status) VALUES (1,'社区自提点','社区路1号',1)");
        fixtures.update("INSERT INTO dish (id,name,price,status,stock) VALUES (1,'番茄',12.50,1,5)");
        String result = mvc.perform(post("/api/orders").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"price-case-1\",\"pickupPointId\":1,"
                                + "\"pickupPersonName\":\"测试用户\",\"pickupPersonPhone\":\"13800000011\","
                                + "\"amount\":0.01,\"packAmount\":-100,\"items\":[{\"dishId\":1,\"number\":2,"
                                + "\"name\":\"客户端伪造名称\",\"price\":0.01}]}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderAmount").value(25.00))
                .andReturn().getResponse().getContentAsString();
        String id = com.alibaba.fastjson.JSON.parseObject(result).getJSONObject("data").getString("id");
        mvc.perform(get("/api/orders/" + id).header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.amount").value(25.00))
                .andExpect(jsonPath("$.data.orderDetailList[0].name").value("番茄"))
                .andExpect(jsonPath("$.data.orderDetailList[0].amount").value(12.50));
    }

    @Test
    void retryingCheckoutReturnsSameOrderAndRejectsReusingKeyForDifferentItems() throws Exception {
        checkoutFixtures(5);
        String first = checkout("intent-1", 2, 200);
        String retry = checkout("intent-1", 2, 200);
        assertEquals(first, retry);
        checkout("intent-1", 1, 400);
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void insufficientStockRejectsWholeCheckoutWithoutCreatingAnOrder() throws Exception {
        checkoutFixtures(3);
        checkout("stock-1", 2, 200);
        checkout("stock-2", 2, 400);
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void cancellingAnOrderRestoresItsOriginalReservationOnlyOnce() throws Exception {
        checkoutFixtures(3);
        String id = checkout("cancel-stock-1", 2, 200);
        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200));
        checkout("cancel-stock-2", 3, 200);
        checkout("cancel-stock-3", 1, 400);
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "1.5", "10000"})
    void checkoutRejectsNonpositiveFractionalAndExcessiveQuantities(String number) throws Exception {
        checkoutFixtures(3);
        mvc.perform(post("/api/orders").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clientRequestId\":\"invalid-qty\","
                                + "\"pickupPointId\":1,\"pickupPersonName\":\"测试\",\"pickupPersonPhone\":\"13800000011\","
                                + "\"items\":[{\"dishId\":1,\"number\":" + number + "}]}"))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void failureOnSecondProductRollsBackTheFirstProductsReservation() throws Exception {
        checkoutFixtures(1);
        fixtures.update("INSERT INTO dish (id,name,price,status,stock) VALUES (2,'青菜',4.00,1,0)");
        mvc.perform(post("/api/orders").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clientRequestId\":\"rollback-1\","
                                + "\"pickupPointId\":1,\"pickupPersonName\":\"测试\",\"pickupPersonPhone\":\"13800000011\","
                                + "\"items\":[{\"dishId\":1,\"number\":1},{\"dishId\":2,\"number\":1}]}"))
                .andExpect(jsonPath("$.code").value(400));
        checkout("rollback-2", 1, 200);
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @ParameterizedTest
    @CsvSource({"1,0", "6,1", "5,1", "2,0"})
    void administratorCannotAcceptUnpaidOrTerminalOrders(int orderStatus, int payStatus) throws Exception {
        seedOrder(11L, orderStatus, payStatus, null);
        mvc.perform(put("/admin/order/confirm").header("Authorization", adminToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":101}"))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(orderStatus));
    }

    @ParameterizedTest
    @CsvSource({"6,1", "5,1", "4,1", "1,1", "2,0"})
    void pickupVerificationRejectsInvalidOrderAndPaymentStates(int orderStatus, int payStatus) throws Exception {
        seedOrder(11L, orderStatus, payStatus, "12345678");
        mvc.perform(post("/admin/order/verify").header("Authorization", adminToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pickupCode\":\"12345678\",\"pickupPointId\":1}"))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(orderStatus))
                .andExpect(jsonPath("$.data.verifyStatus").value(0));
    }

    @ParameterizedTest
    @CsvSource({"/admin/order/delivery/101,3,1,1", "/admin/order/delivery/101,3,0,0",
            "/admin/order/complete/101,4,0,0", "/admin/order/cancel,5,1,0"})
    void administratorCannotSkipFulfillmentAndCancellationRules(String path, int orderStatus, int payStatus, int pickup) throws Exception {
        seedOrder(11L, orderStatus, payStatus, null);
        if (pickup == 0) fixtures.update("UPDATE orders SET pickup_point_id=NULL WHERE id=101");
        mvc.perform(put(path).header("Authorization", adminToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":101,\"cancelReason\":\"测试取消\"}"))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(orderStatus));
    }

    @ParameterizedTest
    @CsvSource({"/admin/order/cancel", "/admin/order/rejection"})
    void administratorCancellationRecordsMockRefundAndReleasesStockOnce(String path) throws Exception {
        checkoutFixtures(3);
        String id = checkout("admin-cancel-1", 2, 200);
        mvc.perform(post("/api/pay/" + id + "/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200));
        String body = "{\"id\":" + id + ",\"cancelReason\":\"测试取消\",\"rejectionReason\":\"测试拒单\"}";
        mvc.perform(put(path).header("Authorization", adminToken(1L)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(1));
        mvc.perform(get("/api/orders/" + id).header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(6))
                .andExpect(jsonPath("$.data.payStatus").value(2));
        mvc.perform(put(path).header("Authorization", adminToken(1L)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(1));
        checkout("admin-cancel-2", 3, 200);
        checkout("admin-cancel-3", 1, 400);
    }

    @ParameterizedTest
    @CsvSource({"1,200,3,6", "2,400,2,2"})
    void userCancellationRespectsTheGroupLifecycle(int groupStatus, int expectedCode, int expectedGroupStatus, int expectedOrderStatus) throws Exception {
        checkoutFixtures(1);
        fixtures.update("INSERT INTO groupon(id,dish_id,leader_user_id,groupon_price,required_size,current_size,status,expire_time,create_time) "
                + "VALUES(100,1,11,10.00,2,1,?,DATEADD('DAY',1,CURRENT_TIMESTAMP),CURRENT_TIMESTAMP)", groupStatus);
        seedOrder(11L, 2, 1, "12345678");
        fixtures.update("UPDATE orders SET is_groupon=1,groupon_id=100 WHERE id=101");
        fixtures.update("INSERT INTO groupon_member(groupon_id,user_id,order_id,is_leader,number,amount,create_time) VALUES(100,11,101,1,2,20.00,CURRENT_TIMESTAMP)");
        fixtures.update("INSERT INTO order_stock(order_id,dish_id,number) VALUES(101,1,2)");
        mvc.perform(post("/api/orders/101/cancel").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(expectedCode));
        assertEquals(expectedGroupStatus, groups.getDetail(100L).getStatus());
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(expectedOrderStatus));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void expiredUnpaidOrdersCannotBePaidBeforeTheExpiryWorkerRuns(boolean groupOrder) throws Exception {
        seedOrder(11L, 1, 0, null);
        if (groupOrder) {
            fixtures.update("INSERT INTO groupon(id,dish_id,leader_user_id,groupon_price,required_size,current_size,status,expire_time,create_time) "
                    + "VALUES(100,1,11,10.00,2,1,1,'2000-01-01 00:00:00','2000-01-01 00:00:00')");
            fixtures.update("UPDATE orders SET is_groupon=1,groupon_id=100 WHERE id=101");
        } else fixtures.update("UPDATE orders SET order_time='2000-01-01 00:00:00' WHERE id=101");
        mvc.perform(post("/api/pay/101/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.payStatus").value(0))
                .andExpect(jsonPath("$.data.pickupCode").isEmpty());
    }

    @Test
    void deliveryPaymentDoesNotIssueAPickupCredential() throws Exception {
        seedOrder(11L, 1, 0, null);
        fixtures.update("UPDATE orders SET pickup_point_id=NULL WHERE id=101");
        mvc.perform(post("/api/pay/101/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.payStatus").value(1))
                .andExpect(jsonPath("$.data.pickupCode").isEmpty());
    }

    @Test
    void expiryReleasesAnUnpaidOrdersReservationOnceAndPreservesPaidOrders() throws Exception {
        checkoutFixtures(1);
        seedOrder(11L, 1, 0, null);
        fixtures.update("UPDATE orders SET order_time='2000-01-01 00:00:00' WHERE id=101");
        fixtures.update("INSERT INTO order_stock(order_id,dish_id,number) VALUES(101,1,2)");
        fixtures.update("INSERT INTO orders(id,number,user_id,status,pay_status,amount,order_time) VALUES(102,'paid-order',11,2,1,12.50,'2000-01-01 00:00:00')");
        org.junit.jupiter.api.Assertions.assertTrue(orderService.expireUnpaidOrder(101L));
        org.junit.jupiter.api.Assertions.assertFalse(orderService.expireUnpaidOrder(101L));
        org.junit.jupiter.api.Assertions.assertFalse(orderService.expireUnpaidOrder(102L));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(6));
        mvc.perform(get("/api/orders/102").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(2)).andExpect(jsonPath("$.data.payStatus").value(1));
        checkout("after-expiry-1", 3, 200);
        checkout("after-expiry-2", 1, 400);
    }

    private void checkoutFixtures(int stock) {
        fixtures.update("INSERT INTO user (id,phone) VALUES (11,'13800000011')");
        fixtures.update("INSERT INTO pickup_point (id,name,address,status) VALUES (1,'社区自提点','社区路1号',1)");
        fixtures.update("INSERT INTO dish (id,name,price,status,stock) VALUES (1,'番茄',12.50,1,?)", stock);
    }

    private String checkout(String key, int quantity, int code) throws Exception {
        String body = "{\"clientRequestId\":\"" + key + "\",\"pickupPointId\":1,"
                + "\"pickupPersonName\":\"测试用户\",\"pickupPersonPhone\":\"13800000011\","
                + "\"items\":[{\"dishId\":1,\"number\":" + quantity + "}]}";
        String result = mvc.perform(post("/api/orders").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(code)).andReturn().getResponse().getContentAsString();
        return code == 200 ? com.alibaba.fastjson.JSON.parseObject(result).getJSONObject("data").getString("id") : null;
    }

    @Test
    void emptyOrderHistoryContainsNoDemonstrationOrders() throws Exception {
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void nonexistentOrderDoesNotGeneratePickupCode() throws Exception {
        mvc.perform(post("/api/pay/999/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/orders/101,400",
            "GET,/user/order/orderDetail/101,0",
            "POST,/api/orders/101/cancel,400",
            "PUT,/user/order/cancel/101,0",
            "POST,/api/pay/101/mock-pay,400",
            "POST,/user/order/101/mock-pay,0",
            "PUT,/user/order/payment,0",
            "POST,/user/order/repetition/101,0",
            "GET,/user/order/reminder/101,0"
    })
    void usersCannotReadOrMutateAnotherUsersOrder(String method, String path, int failureCode) throws Exception {
        fixtures.update("INSERT INTO orders (id,number,user_id,status,pay_status,amount,order_time) "
                + "VALUES (101,'order-101',22,1,0,19.90,CURRENT_TIMESTAMP)");
        mvc.perform(request(HttpMethod.valueOf(method), path).header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"order-101\",\"payMethod\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(failureCode));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(22L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.payStatus").value(0));
    }

    @Test
    void legacyPaymentCannotReviveCancelledOrder() throws Exception {
        seedOrder(11L, 6, 0, null);
        mvc.perform(put("/user/order/payment").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"order-101\",\"payMethod\":1}"))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(6))
                .andExpect(jsonPath("$.data.payStatus").value(0));
    }

    @Test
    void legacyPaymentReturnsThePersistedPickupCodeOnEveryIdempotentRetry() throws Exception {
        checkoutFixtures(3);
        String id = checkout("legacy-pay-1", 2, 200);
        String orderNumber = orderNumber(id);
        for (int attempt = 0; attempt < 3; attempt++) {
            String dbg = mvc.perform(put("/user/order/payment").header("Authorization", userToken(11L))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderNumber\":\"" + orderNumber + "\",\"payMethod\":1}"))
                    .andExpect(jsonPath("$.code").value(1))
                    .andReturn().getResponse().getContentAsString();
            System.out.println("DBG-PAYMENT-BODY=" + dbg);
            org.junit.jupiter.api.Assertions.assertEquals(storedPickupCode(id),
                    com.jayway.jsonpath.JsonPath.read(dbg, "$.data.pickupCode"));
        }
        // 新入口同一契约：重复支付返回同一持久化码（data 直接是码），终态不回退
        mvc.perform(post("/api/pay/" + id + "/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data").value(storedPickupCode(id)));
        mvc.perform(get("/api/orders/" + id).header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.payStatus").value(1));
    }

    private String orderNumber(String id) {
        return fixtures.queryForObject("select number from orders where id = " + id, String.class);
    }

    private String storedPickupCode(String id) {
        return fixtures.queryForObject("select pickup_code from orders where id = " + id, String.class);
    }

    @Test
    void previousAuthenticatedRequestCannotAuthorizeAnonymousPasswordChange() throws Exception {
        fixtures.update("INSERT INTO user (id,phone,password) VALUES (11,'13800000011','unchanged')");
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/auth/set-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"new-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/user/addressBook/202",
            "PUT,/user/addressBook",
            "DELETE,/user/addressBook?id=202",
            "PUT,/user/addressBook/default"
    })
    void usersCannotReadOrMutateAnotherUsersAddress(String method, String path) throws Exception {
        fixtures.update("INSERT INTO address_book (id,user_id,consignee,detail,is_default) "
                + "VALUES (201,11,'Alice','original-alice',1),(202,22,'Bob','original-bob',1)");
        mvc.perform(request(HttpMethod.valueOf(method), path).header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":202,\"userId\":11,\"detail\":\"changed\",\"isDefault\":1}"))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/user/addressBook/202").header("Authorization", userToken(22L)))
                .andExpect(jsonPath("$.data.detail").value("original-bob"));
        mvc.perform(get("/user/addressBook/default").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.id").value("201"));
    }

    @Test
    void incorrectDevelopmentCodeCannotIssueUserToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000011\",\"code\":\"wrong-code\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void storedPasswordHashCannotBeUsedAsLoginPassword() throws Exception {
        fixtures.update("INSERT INTO user (id,phone,password) VALUES "
                + "(11,'13800000011','5f4dcc3b5aa765d61d8327deb882cf99')");
        mvc.perform(post("/api/auth/login-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000011\",\"password\":\"5f4dcc3b5aa765d61d8327deb882cf99\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void largeIdentifiersAndTimeRetainContractPrecision() throws Exception {
        fixtures.update("INSERT INTO orders (id,number,user_id,status,pay_status,amount,order_time) "
                + "VALUES (9007199254740993,'large-order',11,1,0,19.90,'2026-10-24 12:34:56')");
        mvc.perform(get("/api/orders/9007199254740993").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.id").value("9007199254740993"))
                .andExpect(jsonPath("$.data.orderTime").value("2026-10-24 12:34:56"));
    }

    @Test
    void authenticatedUserCanSetPasswordAndLoginWithIt() throws Exception {
        fixtures.update("INSERT INTO user (id,phone) VALUES (11,'13800000011')");
        mvc.perform(post("/api/auth/set-password").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"new-password\"}"))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/auth/login-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000011\",\"password\":\"new-password\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("11"))
                .andExpect(jsonPath("$.data.token").isString());
        mvc.perform(post("/api/auth/login-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000011\",\"password\":\"wrong-password\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void legacyPasswordStillWorksAfterMigration() throws Exception {
        fixtures.update("INSERT INTO user (id,phone,password) VALUES "
                + "(11,'13800000011','5f4dcc3b5aa765d61d8327deb882cf99')");
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/auth/login-password").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"13800000011\",\"password\":\"password\"}"))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value("11"));
        }
    }

    @Test
    void validDevelopmentCodeReturnsExplicitMockLogin() throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000011\",\"code\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mock").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.token");
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void administratorCanReadOrdersButUserTokenCannotActAsAdministrator() throws Exception {
        seedOrder(22L, 1, 0, null);
        mvc.perform(get("/admin/order/details/101").header("Authorization", userToken(11L)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/order/details/101").header("Authorization", adminToken(11L)))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.userId").value("22"));
        assertNull(BaseContext.getCurrentId(), "Administrator identity must not outlive its request");
        mvc.perform(get("/api/orders").header("Authorization", adminToken(11L)))
                .andExpect(status().isUnauthorized());
        assertNull(BaseContext.getCurrentId());
    }

    @ParameterizedTest
    @CsvSource({"2", "3", "4", "5"})
    void repeatedPaymentPreservesPaidOrderState(int originalStatus) throws Exception {
        seedOrder(11L, originalStatus, 1, "12345678");
        mvc.perform(post("/api/pay/101/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("12345678"));
        mvc.perform(put("/user/order/payment").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderNumber\":\"order-101\"}"))
                .andExpect(jsonPath("$.code").value(1));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(originalStatus))
                .andExpect(jsonPath("$.data.pickupCode").value("12345678"));
    }

    @Test
    void firstAndRepeatedMockPaymentReturnSamePickupCode() throws Exception {
        seedOrder(11L, 1, 0, null);
        String first = mvc.perform(post("/api/pay/101/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.matchesPattern("\\d{8}")))
                .andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/pay/101/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        String firstCode = com.jayway.jsonpath.JsonPath.read(first, "$.data");
        String secondCode = com.jayway.jsonpath.JsonPath.read(second, "$.data");
        assertEquals(firstCode, secondCode);
    }

    @ParameterizedTest
    @CsvSource({"0", "1", "2"})
    void cancelledOrdersCannotBePaidRegardlessOfPaymentFlag(int paymentFlag) throws Exception {
        seedOrder(11L, 6, paymentFlag, "12345678");
        mvc.perform(post("/api/pay/101/mock-pay").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(6));
    }

    @ParameterizedTest
    @CsvSource({"3", "4", "5"})
    void userCancellationCannotChangeOrdersAfterAcceptance(int originalStatus) throws Exception {
        seedOrder(11L, originalStatus, 1, "12345678");
        mvc.perform(post("/api/orders/101/cancel").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(originalStatus));
    }

    @Test
    void repeatedUserCancellationOnlyRecordsOneSimulatedRefund() throws Exception {
        seedOrder(11L, 2, 1, "12345678");
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/orders/101/cancel").header("Authorization", userToken(11L)))
                    .andExpect(jsonPath("$.code").value(200));
        }
        mvc.perform(get("/api/orders/101").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.status").value(6))
                .andExpect(jsonPath("$.data.payStatus").value(2));
    }

    @Test
    void orderPaginationRetainsNumericCountsInBothCompatibleEnvelopes() throws Exception {
        seedOrder(11L, 1, 0, null);
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.data.total").isNumber())
                .andExpect(jsonPath("$.data.records[0].id").value("101"));
    }

    @Test
    void ownerCanChangeDefaultAddressWithoutChangingAddressDetails() throws Exception {
        fixtures.update("INSERT INTO address_book (id,user_id,consignee,detail,is_default) "
                + "VALUES (201,11,'Alice','first-address',1),(202,11,'Alice','second-address',0)");
        mvc.perform(put("/user/addressBook/default").header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":202,\"detail\":\"untrusted-change\"}"))
                .andExpect(jsonPath("$.code").value(1));
        mvc.perform(get("/user/addressBook/default").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.id").value("202"))
                .andExpect(jsonPath("$.data.detail").value("second-address"));
        mvc.perform(get("/user/addressBook/list").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data[?(@.isDefault == 1)]", org.hamcrest.Matchers.hasSize(1)));
    }

    @ParameterizedTest
    @CsvSource({"/user/order/submit,0", "/api/orders,400"})
    void usersCannotSubmitOrdersWithAnotherUsersAddress(String path, int failureCode) throws Exception {
        fixtures.update("INSERT INTO address_book (id,user_id,consignee,phone,detail,is_default) "
                + "VALUES (202,22,'Bob','13800000022','private-address',1)");
        fixtures.update("INSERT INTO shopping_cart (id,user_id,dish_id,name,number,amount) "
                + "VALUES (301,11,1,'Vegetables',1,19.90)");
        mvc.perform(post(path).header("Authorization", userToken(11L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressBookId\":202,\"amount\":19.90,"
                                + "\"items\":[{\"dishId\":1,\"quantity\":1,\"price\":19.90}]}"))
                .andExpect(jsonPath("$.code").value(failureCode));
        mvc.perform(get("/api/orders").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void identifierSerializationKeepsCountdownDurationNumeric() throws Exception {
        com.sky.vo.GrouponVO group = com.sky.vo.GrouponVO.builder()
                .id(9007199254740993L).remainMillis(123000L).build();
        com.sky.json.JacksonObjectMapper mapper = new com.sky.json.JacksonObjectMapper();
        com.fasterxml.jackson.databind.JsonNode response = mapper.readTree(mapper.writeValueAsString(group));
        assertEquals("9007199254740993", response.path("id").textValue());
        org.junit.jupiter.api.Assertions.assertTrue(response.path("remainMillis").isNumber(),
                "A countdown duration is a quantity, not an identifier");
        assertEquals(123000, response.path("remainMillis").longValue());
    }

    private String adminToken(long id) {
        return "Bearer " + JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), 60000,
                new java.util.HashMap<>(Collections.singletonMap("empId", id)));
    }

    private void seedOrder(long userId, int orderStatus, int payStatus, String pickupCode) {
        fixtures.update("INSERT INTO orders (id,number,user_id,status,pay_status,amount,order_time,"
                        + "pickup_point_id,pickup_code) VALUES (101,'order-101',?,?,?,?,CURRENT_TIMESTAMP,1,?)",
                userId, orderStatus, payStatus, new java.math.BigDecimal("19.90"), pickupCode);
    }

    private String userToken(long id) {
        return "Bearer " + JwtUtil.createJWT(jwtProperties.getUserSecretKey(), 60000,
                new java.util.HashMap<>(Collections.singletonMap("userId", id)));
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan("com.sky.mapper")
    @Import({WebMvcConfiguration.class, JwtTokenUserInterceptor.class, JwtTokenAdminInterceptor.class,
            ApiOrderController.class, ApiPayController.class, ApiAuthController.class,
            AddressBookController.class, com.sky.controller.user.OrderController.class,
            com.sky.controller.admin.OrderController.class, OrderServiceImpl.class, com.sky.service.CheckoutService.class, com.sky.service.impl.GrouponServiceImpl.class,
            AddressBookServiceImpl.class, GlobalExceptionHandler.class, ResultResponseAdvice.class,
            WebSocketServer.class})
    static class TestConfiguration {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .setName("contracts-" + java.util.UUID.randomUUID()
                            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER")
                    .addScript("classpath:contracts-schema.sql").addScript("classpath:groupon-contracts-schema.sql").build();
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setTypeAliasesPackage("com.sky.entity");
            factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                    .getResources("classpath:mapper/*.xml"));
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(configuration);
            PageInterceptor pagination = new PageInterceptor();
            Properties properties = new Properties();
            properties.setProperty("helperDialect", "mysql");
            pagination.setProperties(properties);
            factory.setPlugins(pagination);
            return factory.getObject();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setUserSecretKey("contract-test-user-secret-not-for-production");
            properties.setAdminSecretKey("contract-test-admin-secret-not-for-production");
            properties.setUserTokenName("authentication");
            properties.setAdminTokenName("token");
            properties.setUserTtl(60000);
            properties.setAdminTtl(60000);
            return properties;
        }

        @Bean
        WeChatPayUtil weChatPayUtil() {
            // The only mock is the external payment gateway. No production credentials are loaded.
            return org.mockito.Mockito.mock(WeChatPayUtil.class);
        }

        @Bean
        com.sky.properties.WeChatProperties weChatProperties() {
            return new com.sky.properties.WeChatProperties();
        }
    }
}
