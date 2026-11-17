package io.github.hoangluongtran0309.vietnam.demo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "vietnam.zalo-oa.enabled=false")
class ReconciliationDemoControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void createsSyntheticCheckout() throws Exception {
        MvcResult result = mockMvc.perform(get("/checkout").param("amount", "150000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(150000))
                .andExpect(jsonPath("$.qrPayload").isNotEmpty())
                .andReturn();

        String reference = JsonPath.read(result.getResponse().getContentAsString(), "$.reference");
        assertThat(reference).startsWith("DH");
    }

    @Test
    void simulatesMatchedPayment() throws Exception {
        String reference = checkoutReference(150_000);

        MvcResult result = simulate("MBVCB.1234." + reference + ".CT tu demo", 150_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("MATCHED"))
                .andExpect(jsonPath("$.reference").value(reference))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("0123456789", "970436");
    }

    @Test
    void simulatesUnderpaidPayment() throws Exception {
        String reference = checkoutReference(150_000);

        simulate(reference, 100_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("UNDERPAID"))
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.missingAmount").value(50_000));
    }

    @Test
    void simulatesOverpaidPayment() throws Exception {
        String reference = checkoutReference(150_000);

        simulate(reference, 175_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("OVERPAID"))
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.excessAmount").value(25_000));
    }

    @Test
    void simulatesUnmatchedPaymentWithoutEchoingContent() throws Exception {
        String sensitiveContent = "payer supplied transfer text";

        MvcResult result = simulate(sensitiveContent, 150_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("UNMATCHED"))
                .andExpect(jsonPath("$.reason").value("NO_REFERENCE"))
                .andExpect(jsonPath("$.candidates").isEmpty())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(sensitiveContent);
    }

    @Test
    void rejectsNonPositiveAmounts() throws Exception {
        mockMvc.perform(get("/checkout").param("amount", "0"))
                .andExpect(status().isBadRequest());
        simulate("anything", -1)
                .andExpect(status().isBadRequest());
    }

    private String checkoutReference(long amount) throws Exception {
        MvcResult result = mockMvc.perform(get("/checkout").param("amount", Long.toString(amount)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.reference");
    }

    private org.springframework.test.web.servlet.ResultActions simulate(String content, long amount) throws Exception {
        String request = "{\"content\":\"" + content + "\",\"amount\":" + amount + "}";
        return mockMvc.perform(post("/demo/payments/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }
}
