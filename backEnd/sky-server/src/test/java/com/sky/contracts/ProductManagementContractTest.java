package com.sky.contracts;

import com.github.pagehelper.PageInterceptor;
import com.sky.context.BaseContext;
import com.sky.controller.admin.DishController;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.handler.GlobalExceptionHandler;
import com.sky.handler.ResultResponseAdvice;
import com.sky.json.JacksonObjectMapper;
import com.sky.service.DishService;
import com.sky.service.impl.DishServiceImpl;
import com.sky.vo.DishVO;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Public service and HTTP behavior against real MyBatis and isolated H2. */
@SpringJUnitConfig(ProductManagementContractTest.ConfigurationUnderTest.class)
class ProductManagementContractTest {
    @Autowired private DishService products;
    @Autowired private DishController controller;
    @Autowired private DataSource dataSource;
    private JdbcTemplate fixtures;
    private MockMvc mvc;

    @BeforeEach void prepare() {
        fixtures = new JdbcTemplate(dataSource);
        for (String table : new String[]{"order_stock", "orders", "dish_flavor", "setmeal_dish", "dish", "category"}) {
            fixtures.update("DELETE FROM " + table);
        }
        fixtures.update("INSERT INTO category(id,name,type,status) VALUES(1,'Produce',1,1)");
        BaseContext.setCurrentId(7L);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseAdvice())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new JacksonObjectMapper())).build();
    }

    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }

    @Test void minimalProductUsesExplicitSafeDefaultsAndCanBeReadBack() {
        products.saveWithFlavor(product("Default apple"));
        DishVO stored = find("Default apple");
        assertEquals(0, stored.getStatus());
        assertEquals(0, stored.getStock());
        assertEquals(1, stored.getIsFresh());
        assertEquals(0, stored.getIsGroupon());
        assertEquals(new BigDecimal("7.25"), stored.getPrice());
    }

    @Test void aProductRequiresANameACategoryAndAPositivePrice() {
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(product(null)));
        DishDTO withoutPrice = product("No price"); withoutPrice.setPrice(null);
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(withoutPrice));
        DishDTO freePrice = product("Free price"); freePrice.setPrice(new BigDecimal("0.00"));
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(freePrice));
        DishDTO withoutCategory = product("No category"); withoutCategory.setCategoryId(null);
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(withoutCategory));
        DishDTO unknownCategory = product("Unknown category"); unknownCategory.setCategoryId(99L);
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(unknownCategory));
        assertEquals(0, dishCount());
    }

    @Test void stockAndGrouponParametersMustBeValid() {
        DishDTO negativeStock = product("Negative stock"); negativeStock.setStock(-1);
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(negativeStock));
        DishDTO grouponWithoutPrice = product("Groupon without price"); grouponWithoutPrice.setIsGroupon(1);
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(grouponWithoutPrice));
        DishDTO tinyGroup = product("Groupon of one"); tinyGroup.setIsGroupon(1);
        tinyGroup.setGrouponPrice(new BigDecimal("5.00")); tinyGroup.setGrouponSize(1); tinyGroup.setGrouponValidHours(24);
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(tinyGroup));
        DishDTO instantExpiry = product("Groupon without hours"); instantExpiry.setIsGroupon(1);
        instantExpiry.setGrouponPrice(new BigDecimal("5.00")); instantExpiry.setGrouponSize(2); instantExpiry.setGrouponValidHours(0);
        assertThrows(com.sky.exception.BaseException.class, () -> products.saveWithFlavor(instantExpiry));
        DishDTO validGroupon = product("Valid groupon"); validGroupon.setIsGroupon(1);
        validGroupon.setGrouponPrice(new BigDecimal("5.00")); validGroupon.setGrouponSize(2); validGroupon.setGrouponValidHours(24);
        validGroupon.setStock(10);
        products.saveWithFlavor(validGroupon);
        DishVO stored = find("Valid groupon");
        assertEquals(1, stored.getIsGroupon());
        assertEquals(10, stored.getStock());
        assertEquals(1, stored.getIsFresh(), "isFresh must survive a full save");
    }

    @Test void flavorsRollBackTogetherWithTheirProductWhenTheInsertFails() {
        DishDTO broken = product("Broken flavors");
        com.sky.entity.DishFlavor flavor = com.sky.entity.DishFlavor.builder().value("half sweet").build();
        broken.setFlavors(new java.util.ArrayList<>(java.util.Collections.singletonList(flavor)));
        assertThrows(Exception.class, () -> products.saveWithFlavor(broken));
        assertEquals(0, dishCount(), "The dish must not survive a failed flavor insert");
    }

    @Test void updatingOrDeletingAMissingProductFailsExplicitly() {
        DishDTO ghost = product("Ghost"); ghost.setId(999L);
        assertThrows(com.sky.exception.BaseException.class, () -> products.updateWithFlavor(ghost));
        assertThrows(com.sky.exception.BaseException.class, () -> products.deleteBatch(new Long[]{999L}));
    }

    @Test void aPartialUpdateIsValidatedAgainstTheMergedFinalStateNotJustThePayload() {
        products.saveWithFlavor(grouponProduct("Merged groupon"));
        Long id = find("Merged groupon").getId();
        BigDecimal grouponPrice = new BigDecimal("5.00");

        // 省略 isGroupon（MyBatis 对 null 保留原值 1）时，合并后的最终状态仍必须合法
        DishDTO negativePrice = baseUpdate(id, "Merged groupon");
        negativePrice.setGrouponPrice(new BigDecimal("-1"));
        assertThrows(com.sky.exception.BaseException.class, () -> products.updateWithFlavor(negativePrice));
        DishDTO tinyGroup = baseUpdate(id, "Merged groupon"); tinyGroup.setGrouponSize(1);
        assertThrows(com.sky.exception.BaseException.class, () -> products.updateWithFlavor(tinyGroup));
        DishDTO instantExpiry = baseUpdate(id, "Merged groupon"); instantExpiry.setGrouponValidHours(0);
        assertThrows(com.sky.exception.BaseException.class, () -> products.updateWithFlavor(instantExpiry));

        DishVO after = products.getByIdWithFlavor(id);
        assertEquals(grouponPrice, after.getGrouponPrice(), "A rejected groupon price must not be persisted");
        assertEquals(2, after.getGrouponSize(), "A rejected groupon size must not be persisted");
        assertEquals(24, after.getGrouponValidHours(), "A rejected groupon duration must not be persisted");
        assertEquals(1, after.getIsGroupon(), "An omitted flag keeps the stored groupon mode");
        assertEquals(new BigDecimal("7.25"), after.getPrice());

        // 合法部分更新：省略 isGroupon 与全部团参数，原有拼团状态保持不变
        products.updateWithFlavor(baseUpdate(id, "Merged groupon renamed"));
        DishVO afterLegal = products.getByIdWithFlavor(id);
        assertEquals(1, afterLegal.getIsGroupon());
        assertEquals(grouponPrice, afterLegal.getGrouponPrice());
        assertEquals("Merged groupon renamed", afterLegal.getName());

        // 合法降级：显式 isGroupon=0 退出拼团模式，省略的团参数不再保留陈旧值
        DishDTO downgrade = baseUpdate(id, "Merged groupon renamed"); downgrade.setIsGroupon(0);
        products.updateWithFlavor(downgrade);
        DishVO afterDowngrade = products.getByIdWithFlavor(id);
        assertEquals(0, afterDowngrade.getIsGroupon());
        assertNull(afterDowngrade.getGrouponPrice(), "Leaving groupon mode must not carry a stale price");

        // 提供的非法团参数即使退出拼团模式也拒绝，不让垃圾值入库
        DishDTO downgradeWithGarbage = baseUpdate(id, "Merged groupon renamed");
        downgradeWithGarbage.setIsGroupon(0); downgradeWithGarbage.setGrouponPrice(new BigDecimal("-1"));
        assertThrows(com.sky.exception.BaseException.class, () -> products.updateWithFlavor(downgradeWithGarbage));
    }

    /** 一份保存时合法、但更新载荷带非法拼团字段的现有拼团商品。 */
    private DishDTO grouponProduct(String name) {
        DishDTO request = product(name);
        request.setIsGroupon(1);
        request.setGrouponPrice(new BigDecimal("5.00"));
        request.setGrouponSize(2);
        request.setGrouponValidHours(24);
        return request;
    }

    /** 保留必填字段、省略可选字段的最小更新载荷（isGroupon/groupon* 均为 null）。 */
    private DishDTO baseUpdate(Long id, String name) {
        DishDTO request = product(name);
        request.setId(id);
        return request;
    }

    @Test void aProductStillReservedByActiveOrdersCannotBeDeleted() {
        products.saveWithFlavor(product("Reserved apple"));
        Long id = find("Reserved apple").getId();
        fixtures.update("INSERT INTO orders(id,status) VALUES(5,2)");
        fixtures.update("INSERT INTO order_stock(order_id,dish_id,number) VALUES(5,?,2)", id);
        assertThrows(com.sky.exception.BaseException.class, () -> products.deleteBatch(new Long[]{id}));
        assertEquals(id, products.getByIdWithFlavor(id).getId());
        fixtures.update("UPDATE orders SET status=6 WHERE id=5");
        products.deleteBatch(new Long[]{id});
        assertNull(products.getByIdWithFlavor(id).getId(), "A cancelled reservation must release the product for deletion");
    }

    @Test void adminPagingFollowsTheRequestedPageSizeInsteadOfForcing100000() throws Exception {
        products.saveWithFlavor(product("Page apple"));
        products.saveWithFlavor(product("Page banana"));
        products.saveWithFlavor(product("Page cherry"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/admin/dish/page").param("page", "1").param("pageSize", "2"))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test void anInvalidProductPayloadReturnsTheAdminFailureContract() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/dish").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":7.25,\"categoryId\":1}"))
                .andExpect(jsonPath("$.code").value(0));
    }

    private int dishCount() {
        Integer count = fixtures.queryForObject("select count(*) from dish", Integer.class);
        return count == null ? 0 : count;
    }

    private DishDTO product(String name) {
        DishDTO request = new DishDTO();
        request.setName(name); request.setCategoryId(1L); request.setPrice(new BigDecimal("7.25"));
        return request;
    }

    private DishVO find(String name) {
        DishPageQueryDTO query = new DishPageQueryDTO();
        query.setName(name); query.setPage(1); query.setPageSize(10);
        java.util.List<DishVO> records = products.pageQuery(query).getRecords();
        assertEquals(1, records.size());
        return products.getByIdWithFlavor(records.get(0).getId());
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan("com.sky.mapper")
    @Import({DishServiceImpl.class, DishController.class})
    static class ConfigurationUnderTest {
        @Bean DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .setName("products-" + java.util.UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE")
                    .addScript("classpath:product-contracts-schema.sql").build();
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource); factory.setTypeAliasesPackage("com.sky.entity");
            factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true); factory.setConfiguration(configuration);
            PageInterceptor pagination = new PageInterceptor(); Properties properties = new Properties();
            properties.setProperty("helperDialect", "mysql"); pagination.setProperties(properties); factory.setPlugins(pagination);
            return factory.getObject();
        }
        @Bean PlatformTransactionManager transactionManager(DataSource dataSource) { return new DataSourceTransactionManager(dataSource); }
        @Bean RedisTemplate<?,?> redisTemplate() {
            // Only the external cache is isolated; application services and mappers are real.
            return org.mockito.Mockito.mock(RedisTemplate.class);
        }
    }
}
