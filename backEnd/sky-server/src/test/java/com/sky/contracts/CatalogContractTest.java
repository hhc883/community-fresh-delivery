package com.sky.contracts;

import com.sky.context.BaseContext;
import com.sky.controller.api.ApiDishController;
import com.sky.controller.api.ApiUserController;
import com.sky.controller.api.ApiStoreController;
import com.sky.controller.user.CategoryController;
import com.sky.properties.JwtProperties;
import com.sky.service.impl.CategoryServiceImpl;
import com.sky.service.impl.PickupPointServiceImpl;
import com.sky.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig({BackendContractTest.TestConfiguration.class, CatalogContractTest.ConfigurationForCatalog.class})
@WebAppConfiguration
@TestPropertySource(properties = {"sky.auth.mock-login=true", "sky.auth.dev-code=123456"})
@Sql("classpath:catalog-contracts-schema.sql")
class CatalogContractTest {
    @Autowired private WebApplicationContext context;
    @Autowired private DataSource dataSource;
    @Autowired private JwtProperties jwtProperties;
    private MockMvc mvc;
    private JdbcTemplate fixtures;

    @BeforeEach
    void setUp() {
        BaseContext.removeCurrentId();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        fixtures = new JdbcTemplate(dataSource);
        for (String table : new String[]{"order_stock", "order_detail", "orders", "shopping_cart", "user", "pickup_point", "dish_flavor", "dish", "category"}) {
            fixtures.update("DELETE FROM " + table);
        }
    }

    @AfterEach
    void cleanUp() { BaseContext.removeCurrentId(); }

    @Test
    void anEmptyCatalogReturnsAnActualEmptyList() throws Exception {
        mvc.perform(get("/api/dishes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void catalogVisibilityRequiresAnEnabledDishAndCategory() throws Exception {
        fixtures.update("INSERT INTO category (id,name,status,sort,type) VALUES (1,'Hidden',0,1,1),(2,'Visible',1,2,1)");
        fixtures.update("INSERT INTO dish (id,name,category_id,status,price) VALUES (1,'Hidden category item',1,1,12.50),(2,'Off sale',2,0,12.50),(3,'Real item',2,1,12.50)");
        mvc.perform(get("/api/dishes"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("3"));
        for (long id : new long[]{1, 2, 999}) {
            mvc.perform(get("/api/dishes/" + id)).andExpect(jsonPath("$.code").value(400));
        }
        mvc.perform(get("/api/dishes/3")).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void catalogDoesNotInventSalesDiscountsRatingsOrCategoryLabels() throws Exception {
        fixtures.update("INSERT INTO category (id,name,status,sort,type) VALUES (1,'Fruit',1,1,1)");
        fixtures.update("INSERT INTO dish (id,name,category_id,status,price,sold_count) VALUES (1,'Real apple',1,1,12.50,0)");
        mvc.perform(get("/api/dishes/1"))
                .andExpect(jsonPath("$.data.soldCount").value(0))
                .andExpect(jsonPath("$.data.price").value(12.50))
                .andExpect(jsonPath("$.data.seckillPrice").doesNotExist())
                .andExpect(jsonPath("$.data.goodRate").doesNotExist())
                .andExpect(jsonPath("$.data.promoTag").doesNotExist())
                .andExpect(jsonPath("$.data.category").doesNotExist())
                .andExpect(jsonPath("$.data.data.tag").doesNotExist())
                .andExpect(jsonPath("$.data.data.goodRate").doesNotExist());
    }

    @Test
    void enabledCategoriesCanBeBrowsedWithoutLoggingIn() throws Exception {
        fixtures.update("INSERT INTO category (id,name,status,sort,type) VALUES (1,'Visible',1,1,1),(2,'Hidden',0,2,1)");
        mvc.perform(get("/user/category/list").param("type", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("1"));
    }

    @Test
    void userCenterRequiresAPersistedUserAndCountsOnlyTheirRealOrders() throws Exception {
        mvc.perform(get("/api/user/me").header("Authorization", userToken(999L)))
                .andExpect(jsonPath("$.code").value(400));
        fixtures.update("INSERT INTO user (id,name,phone,password,balance) VALUES (11,'Alice','13800000011','stored-secret',0.00)");
        fixtures.update("INSERT INTO orders (id,number,user_id,status,pay_status,amount,order_time) VALUES (1,'own-a',11,1,0,12.50,CURRENT_TIMESTAMP),(2,'own-b',11,1,0,12.50,CURRENT_TIMESTAMP),(3,'other',22,2,1,12.50,CURRENT_TIMESTAMP)");
        mvc.perform(get("/api/user/me").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("11"))
                .andExpect(jsonPath("$.data.orderCount.pendingPay").value(2))
                .andExpect(jsonPath("$.data.orderCount.pendingPickup").value(0))
                .andExpect(jsonPath("$.data.couponCount").value(0))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void avatarUpdatesRejectInvalidValuesAndCannotChangeCredentials() throws Exception {
        fixtures.update("INSERT INTO user (id,name,phone,password,balance) VALUES (11,'Alice','13800000011','e10adc3949ba59abbe56e057f20f883e',0.00)");
        for (String body : new String[]{"{\"avatar\":\"\"}", "{\"avatar\":13}", "{\"avatar\":\"" + String.join("", Collections.nCopies(501, "x")) + "\"}"}) {
            mvc.perform(post("/api/user/avatar").header("Authorization", userToken(11L))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                    .andExpect(jsonPath("$.code").value(400));
        }
        mvc.perform(post("/api/user/avatar").header("Authorization", userToken(11L))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"avatar\":\"/static/avatar.png\",\"password\":\"attacker\",\"userId\":22}"))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/user/me").header("Authorization", userToken(11L)))
                .andExpect(jsonPath("$.data.avatar").value("/static/avatar.png"))
                .andExpect(jsonPath("$.data.phone").value("13800000011"));
        mvc.perform(post("/api/auth/login-password").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000011\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/dishes/1/reviews", "POST,/api/dishes/1/reviews", "GET,/api/seckill/current",
            "GET,/api/coupons", "GET,/api/coupons/center", "POST,/api/coupons/claim",
            "GET,/api/invite/me", "GET,/api/invite/cash", "POST,/api/invite/cash/withdraw",
            "GET,/api/free/current", "POST,/api/user/change-phone"})
    void unsupportedReviewsAndMarketingReturnExplicitNotImplemented(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path).header("Authorization", userToken(11L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"newPhone\":\"13800000022\",\"couponKey\":\"not-real\"}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value(501));
    }

    @Test
    void storesExposeOnlyEnabledRealRecordsAndNoInventedDistance() throws Exception {
        mvc.perform(get("/api/stores")).andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(0));
        fixtures.update("INSERT INTO pickup_point (id,name,address,status) VALUES (7,'Real pickup point','Real address',1),(8,'Closed point','Closed address',0)");
        mvc.perform(get("/api/stores"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value("7"))
                .andExpect(jsonPath("$.data.records[0].name").value("Real pickup point"))
                .andExpect(jsonPath("$.data.records[0].distance").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].service").doesNotExist());
        mvc.perform(get("/api/stores/7")).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("7"));
        for (long id : new long[]{8, 999}) {
            mvc.perform(get("/api/stores/" + id)).andExpect(jsonPath("$.code").value(400));
        }
    }

    private String userToken(long id) {
        return "Bearer " + JwtUtil.createJWT(jwtProperties.getUserSecretKey(), 60000,
                new HashMap<>(Collections.singletonMap("userId", id)));
    }

    @Configuration
    @Import({ApiDishController.class, ApiUserController.class, ApiStoreController.class,
            CategoryController.class, CategoryServiceImpl.class, PickupPointServiceImpl.class})
    static class ConfigurationForCatalog { }
}
