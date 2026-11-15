package io.github.hoangluongtran0309.vietnam.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "vietnam.zalo-oa.enabled=false")
class ZaloDisabledControllerRegistrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void hidesZaloEndpointsWhenZaloIsDisabled() throws Exception {
        mockMvc.perform(get("/zalo/oauth/start"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/zalo/consultation"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/zalo/zbs"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/zalo/webhook/last"))
                .andExpect(status().isNotFound());
    }
}
