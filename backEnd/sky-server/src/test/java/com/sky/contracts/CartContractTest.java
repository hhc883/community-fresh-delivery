package com.sky.contracts;

import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(CartContractTest.CartConfiguration.class)
@WebAppConfiguration
public class CartContractTest {
    @Configuration
    @Import({BackendContractTest.TestConfiguration.class,
            com.sky.controller.user.ShoppingCardController.class,
            com.sky.service.impl.ShoppingCartServiceImpl.class})
    static class CartConfiguration {}

    @Autowired private WebApplicationContext context;
    @Autowired private DataSource dataSource;
    @Autowired private JwtProperties jwt;
    @Autowired private com.sky.service.ShoppingCartService service;
    private JdbcTemplate fixtures;
    private MockMvc mvc;

    @BeforeEach void prepare() {
        BaseContext.removeCurrentId();
        fixtures = new JdbcTemplate(dataSource);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        for (String table : new String[]{"shopping_cart","order_stock","order_detail","orders","dish","setmeal","setmeal_dish","user","address_book"}) {
            fixtures.update("DELETE FROM " + table);
        }
        fixtures.update("INSERT INTO user(id,name) VALUES(11,'Alice'),(22,'Bob')");
        fixtures.update("INSERT INTO dish(id,name,status,price,stock) VALUES(1,'Apples',1,8.50,10),(2,'Pears',1,3.00,10)");
        fixtures.update("INSERT INTO address_book(id,user_id,consignee,phone,detail) VALUES(11,11,'Alice','13800000011','A')");
        fixtures.update("INSERT INTO shopping_cart(user_id,dish_id,name,number,amount) VALUES(11,1,'Apples',1,8.50)");
    }

    @AfterEach void clear() { BaseContext.removeCurrentId(); }

    @Test void addingTheSameDishNeverChangesAnotherUsersCart() throws Exception {
        mvc.perform(post("/user/shoppingCart/add").header("Authorization",token(22))
                        .contentType("application/json").content("{\"dishId\":1}"))
                .andExpect(jsonPath("$.code").value(1));
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(11)))
                .andExpect(jsonPath("$.data[0].number").value(1));
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(22)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].number").value(1));
    }

    @Test void decrementingAnotherUsersItemLeavesBothCartsIsolated() throws Exception {
        mvc.perform(post("/user/shoppingCart/sub").header("Authorization",token(22))
                        .contentType("application/json").content("{\"dishId\":1}"))
                .andExpect(jsonPath("$.code").value(1));
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].number").value(1));
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(22)))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void bothCleanRoutesDeleteOnlyTheCallersItems() throws Exception {
        for (String endpoint : new String[]{"/user/shoppingCart/delete","/user/shoppingCart/clean"}) {
            fixtures.update("INSERT INTO shopping_cart(user_id,dish_id,name,number,amount) VALUES(22,2,'Pears',1,3.00)");
            mvc.perform(delete(endpoint).header("Authorization",token(22))).andExpect(jsonPath("$.code").value(1));
            mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(22))).andExpect(jsonPath("$.data").isEmpty());
            mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(11)))
                    .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].number").value(1));
        }
    }

    @Test void itemMutationsRejectMissingAmbiguousAndInvalidIdentifiers() throws Exception {
        for (String endpoint : new String[]{"/user/shoppingCart/add","/user/shoppingCart/sub"}) {
            for (String body : new String[]{"{}","{\"dishId\":1,\"setmealId\":1}","{\"dishId\":0}","{\"setmealId\":-1}"}) {
                mvc.perform(post(endpoint).header("Authorization",token(22)).contentType("application/json").content(body))
                        .andExpect(jsonPath("$.code").value(0));
            }
        }
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(11)))
                .andExpect(jsonPath("$.data[0].number").value(1));
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(22))).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void anonymousCallsCannotReadOrMutateAGuestCart() throws Exception {
        org.junit.jupiter.api.Assertions.assertThrows(com.sky.exception.ShoppingCartBusinessException.class, service::showShoppingCart);
        org.junit.jupiter.api.Assertions.assertThrows(com.sky.exception.ShoppingCartBusinessException.class, service::cleanShoppingCart);
        mvc.perform(get("/user/shoppingCart/list")).andExpect(status().isUnauthorized());
        mvc.perform(post("/user/shoppingCart/add").contentType("application/json").content("{\"dishId\":1}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/user/shoppingCart/sub").contentType("application/json").content("{\"dishId\":1}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/user/shoppingCart/clean")).andExpect(status().isUnauthorized());
    }

    @Test void simultaneousAddsCreateOneOwnedLineWithoutLosingQuantity() throws Exception {
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(4);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<?>> results = new java.util.ArrayList<>();
        try {
            for (int i=0; i<4; i++) results.add(workers.submit(() -> {
                try {
                    start.await(10,java.util.concurrent.TimeUnit.SECONDS);
                    mvc.perform(post("/user/shoppingCart/add").header("Authorization",token(22))
                                    .contentType("application/json").content("{\"dishId\":2}"))
                            .andExpect(jsonPath("$.code").value(1));
                } catch (Exception failure) { throw new RuntimeException(failure); }
            }));
            start.countDown();
            for (java.util.concurrent.Future<?> result : results) result.get(20,java.util.concurrent.TimeUnit.SECONDS);
        } finally { workers.shutdownNow(); }
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(22)))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].number").value(4));
    }

    @Test void anItemAddedDuringCheckoutSurvivesForTheNextPurchase() throws Exception {
        PauseOrderInsert.paused = new java.util.concurrent.CountDownLatch(1);
        PauseOrderInsert.release = new java.util.concurrent.CountDownLatch(1);
        fixtures.execute("CREATE TRIGGER pause_checkout BEFORE INSERT ON orders FOR EACH ROW CALL 'com.sky.contracts.CartContractTest$PauseOrderInsert'");
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> purchase = workers.submit(() -> {
                try {
                    mvc.perform(post("/api/orders").header("Authorization",token(11)).contentType("application/json")
                                    .content("{\"clientRequestId\":\"cart-concurrent\",\"addressBookId\":11}"))
                            .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.amount").value(8.50));
                } catch (Exception failure) { throw new RuntimeException(failure); }
            });
            org.junit.jupiter.api.Assertions.assertTrue(PauseOrderInsert.paused.await(10,java.util.concurrent.TimeUnit.SECONDS));
            java.util.concurrent.Future<?> addition = workers.submit(() -> {
                try {
                    mvc.perform(post("/user/shoppingCart/add").header("Authorization",token(11))
                                    .contentType("application/json").content("{\"dishId\":2}"))
                            .andExpect(jsonPath("$.code").value(1));
                } catch (Exception failure) { throw new RuntimeException(failure); }
            });
            try { addition.get(300,java.util.concurrent.TimeUnit.MILLISECONDS); }
            catch (java.util.concurrent.TimeoutException waitingForCheckout) { /* Release the database fixture below. */ }
            PauseOrderInsert.release.countDown();
            purchase.get(20,java.util.concurrent.TimeUnit.SECONDS);
            addition.get(20,java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            PauseOrderInsert.release.countDown();
            workers.shutdownNow();
            fixtures.execute("DROP TRIGGER IF EXISTS pause_checkout");
        }
        mvc.perform(get("/user/shoppingCart/list").header("Authorization",token(11)))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].dishId").value("2"))
                .andExpect(jsonPath("$.data[0].number").value(1));
    }

    /** The isolated database pauses a real checkout after it has read the cart. No business collaborator is mocked. */
    public static class PauseOrderInsert implements org.h2.api.Trigger {
        static java.util.concurrent.CountDownLatch paused;
        static java.util.concurrent.CountDownLatch release;
        @Override public void fire(java.sql.Connection connection, Object[] oldRow, Object[] newRow) throws java.sql.SQLException {
            paused.countDown();
            try {
                if (!release.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new java.sql.SQLException("checkout fixture timed out");
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new java.sql.SQLException(interrupted); }
        }
    }

    private String token(long id) {
        return "Bearer " + JwtUtil.createJWT(jwt.getUserSecretKey(),60000,
                new java.util.HashMap<>(Collections.singletonMap("userId",id)));
    }
}
