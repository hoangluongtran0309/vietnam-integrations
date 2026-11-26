package io.github.hoangluongtran0309.vietnam.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "vietnam.zalo-oa.enabled=false",
        "logging.level.org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor=DEBUG"
})
@ExtendWith(OutputCaptureExtension.class)
class EInvoiceInspectionControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void returnsTheSummaryAndFindingsForAValidInvoice(CapturedOutput output) throws Exception {
        String body = inspectFixture("sales-invoice.xml")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.document.header.formatVersion").value("2.1.0"))
                .andExpect(jsonPath("$.document.header.invoiceSymbol").value("C20TST"))
                .andExpect(jsonPath("$.document.header.currencyCode").value("VND"))
                .andExpect(jsonPath("$.document.lineCount").value(1))
                .andExpect(jsonPath("$.document.totalPaymentAmount").value(90))
                .andExpect(jsonPath("$.document.hasTaxAuthorityCode").value(true))
                .andExpect(jsonPath("$.issues[0].code").value("EINV-SIG-001"))
                .andExpect(jsonPath("$.issues[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.issues[0].path").value("HDon/DSCKS/CQT"))
                .andExpect(jsonPath("$.issues[0].message").isNotEmpty())
                .andExpect(jsonPath("$.signatures").isEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("SYNTHETIC-AUTHORITY-CODE", "<HDon");
        assertThat(output).contains("Writing [InspectionResult]")
                .doesNotContain("C20TST", "SYNTHETIC-AUTHORITY-CODE", "<HDon");
    }

    @Test
    void rejectsDoctypeWithTheStableErrorCode() throws Exception {
        inspect("<!DOCTYPE HDon><HDon/>".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCTYPE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.path").value(nullValue()));
    }

    @Test
    void includesTheElementPathForAnInvalidField() throws Exception {
        byte[] xml = "<HDon><DLHDon><TTChung><NLap>invalid</NLap></TTChung></DLHDon></HDon>"
                .getBytes(StandardCharsets.UTF_8);

        inspect(xml)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE"))
                .andExpect(jsonPath("$.path").value("HDon/DLHDon/TTChung/NLap"));
    }

    @Test
    void reportsTheFakeSignatureAsAnError() throws Exception {
        inspectFixture("fake-signature.xml")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.issues[?(@.code == 'EINV-SIG-002')].severity").value("ERROR"))
                .andExpect(jsonPath("$.signatures[0].role").value("SELLER"))
                .andExpect(jsonPath("$.signatures[0].path").value("HDon/DSCKS/NBan/Signature"))
                .andExpect(jsonPath("$.signatures[0].status").value("MALFORMED"))
                .andExpect(jsonPath("$.signatures[0].trust").value("NOT_CHECKED"))
                .andExpect(jsonPath("$.signatures[0].algorithm").value(nullValue()))
                .andExpect(jsonPath("$.signatures[0].signingTime").value(nullValue()));
    }

    private ResultActions inspectFixture(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/einvoice/synthetic/" + name)) {
            return inspect(input.readAllBytes());
        }
    }

    private ResultActions inspect(byte[] xml) throws Exception {
        return mockMvc.perform(multipart("/einvoice/inspect")
                .file(new MockMultipartFile("file", "invoice.xml", "application/xml", xml)));
    }
}
