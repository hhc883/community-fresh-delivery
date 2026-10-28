package com.sky.contracts;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringJUnitConfig(BackendContractTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {"sky.auth.mock-login=false", "sky.auth.dev-code=123456"})
class MockLoginDisabledContractTest {
    @Autowired private WebApplicationContext context;

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/send-code", "/api/auth/login"})
    void disabledDevelopmentLoginDoesNotClaimSmsSuccessOrIssueToken(String path) throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build()
                .perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000011\",\"code\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
