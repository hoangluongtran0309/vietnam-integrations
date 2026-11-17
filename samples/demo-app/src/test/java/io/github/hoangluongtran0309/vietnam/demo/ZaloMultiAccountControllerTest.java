package io.github.hoangluongtran0309.vietnam.demo;

import com.jayway.jsonpath.JsonPath;
import io.github.hoangluongtran0309.vietnam.zalo.UserTextMessageEvent;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloToken;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "vietnam.zalo-oa.enabled=true",
        "vietnam.zalo-oa.app-id=shared-app-id",
        "vietnam.zalo-oa.secret-key=shared-secret-key",
        "vietnam.zalo-oa.default-account=cskh",
        "vietnam.zalo-oa.accounts.cskh.oa-id=oa-cskh",
        "vietnam.zalo-oa.accounts.marketing.oa-id=oa-marketing"
})
@Import(ZaloMultiAccountControllerTest.OAuthTestConfiguration.class)
class ZaloMultiAccountControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ZaloTokenStore tokenStore;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private FakeZaloOAuthClient oauthClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        oauthClient.clear();
    }

    @Test
    void authorizesEachSelectedAccountAndStoresItsTokenByOaId() throws Exception {
        authorize(null, "customer-care-code", "oa-cskh", "cskh");
        authorize("marketing", "marketing-code", "oa-marketing", "marketing");

        assertThat(tokenStore.load("oa-cskh")).contains(token("customer-care-code"));
        assertThat(tokenStore.load("oa-marketing")).contains(token("marketing-code"));
        assertThat(oauthClient.exchangedCodes())
                .containsExactly("customer-care-code", "marketing-code");
    }

    @Test
    void rejectsCallbackWhenZaloReturnsAnotherAccountId() throws Exception {
        MvcResult start = mockMvc.perform(get("/zalo/oauth/start").param("account", "marketing"))
                .andExpect(status().isOk())
                .andReturn();
        String state = JsonPath.read(start.getResponse().getContentAsString(), "$.state");

        mockMvc.perform(get("/zalo/oauth/callback")
                        .session((MockHttpSession) start.getRequest().getSession(false))
                        .param("code", "authorization-code")
                        .param("oa_id", "oa-cskh")
                        .param("state", state))
                .andExpect(status().isBadRequest());

        assertThat(oauthClient.exchangedCodes()).isEmpty();
    }

    @Test
    void webhookMonitorIncludesTheEventAccountId() throws Exception {
        eventPublisher.publishEvent(new UserTextMessageEvent(
                "shared-app-id", "oa-marketing", "user-id", "oa-marketing",
                Instant.parse("2026-11-17T00:00:00Z"), "message-id", "Xin chao"));

        mockMvc.perform(get("/zalo/webhook/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oaId").value("oa-marketing"));
    }

    private void authorize(String account, String code, String oaId, String expectedAlias) throws Exception {
        var request = get("/zalo/oauth/start");
        if (account != null) {
            request.param("account", account);
        }
        MvcResult start = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value(expectedAlias))
                .andExpect(jsonPath("$.authorizationUrl").value(
                        org.hamcrest.Matchers.containsString("app_id=shared-app-id")))
                .andReturn();
        String state = JsonPath.read(start.getResponse().getContentAsString(), "$.state");

        mockMvc.perform(get("/zalo/oauth/callback")
                        .session((MockHttpSession) start.getRequest().getSession(false))
                        .param("code", code)
                        .param("oa_id", oaId)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("authorized"))
                .andExpect(jsonPath("$.account").value(expectedAlias))
                .andExpect(jsonPath("$.oaId").value(oaId));
    }

    private static ZaloToken token(String prefix) {
        return new ZaloToken(prefix + "-access", prefix + "-refresh",
                Instant.parse("2026-11-18T00:00:00Z"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OAuthTestConfiguration {

        @Bean
        FakeZaloOAuthClient fakeZaloOAuthClient() {
            return new FakeZaloOAuthClient();
        }
    }

    static final class FakeZaloOAuthClient implements ZaloOAuthClient {

        private final List<String> exchangedCodes = new ArrayList<>();

        @Override
        public ZaloToken exchangeAuthorizationCode(String code, String codeVerifier) {
            assertThat(codeVerifier).isNotBlank();
            exchangedCodes.add(code);
            return token(code);
        }

        @Override
        public ZaloToken refresh(String refreshToken) {
            throw new UnsupportedOperationException("Refresh is not used by this test");
        }

        List<String> exchangedCodes() {
            return List.copyOf(exchangedCodes);
        }

        void clear() {
            exchangedCodes.clear();
        }
    }
}
