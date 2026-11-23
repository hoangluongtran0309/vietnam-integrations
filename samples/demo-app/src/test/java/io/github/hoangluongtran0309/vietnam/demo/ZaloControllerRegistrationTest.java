package io.github.hoangluongtran0309.vietnam.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "vietnam.zalo-oa.enabled=true",
        "vietnam.zalo-oa.app-id=test-app-id",
        "vietnam.zalo-oa.secret-key=test-secret-key",
        "vietnam.zalo-oa.oa-id=test-oa-id"
})
class ZaloControllerRegistrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void exposesOAuthStartWhenZaloIsEnabled() throws Exception {
        mockMvc.perform(get("/zalo/oauth/start"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("code_challenge=")));
    }

    @Test
    void exposesMessagingAndWebhookMonitorWhenZaloIsEnabled() throws Exception {
        mockMvc.perform(post("/zalo/consultation"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/zalo/zbs"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/zalo/webhook/last"))
                .andExpect(status().isOk());
    }

    @Test
    void captureModeDoesNotAddBeansWhenCaptureDirectoryIsBlank() {
        assertThat(applicationContext.containsBean("zaloCaptureSession")).isFalse();
        assertThat(applicationContext.getBean("zaloHttpClient").getClass().getSimpleName())
                .doesNotContain("Capturing");
    }
}
