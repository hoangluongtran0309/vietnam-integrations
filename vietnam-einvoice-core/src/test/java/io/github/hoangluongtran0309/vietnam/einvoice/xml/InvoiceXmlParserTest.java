package io.github.hoangluongtran0309.vietnam.einvoice.xml;

import io.github.hoangluongtran0309.vietnam.einvoice.document.CertificateInfo;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceHeader;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceItem;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceParty;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceSignature;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceTotals;
import io.github.hoangluongtran0309.vietnam.einvoice.document.TaxRateSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole.BUYER;
import static io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole.SELLER;
import static io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole.TAX_AUTHORITY;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.DOCTYPE_NOT_ALLOWED;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.DUPLICATE_ELEMENT;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.INPUT_TOO_LARGE;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.INVALID_CERTIFICATE;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.INVALID_DATE;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.INVALID_INTEGER;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.INVALID_NUMBER;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.IO_ERROR;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.MALFORMED_XML;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.TOO_MANY_ELEMENTS;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.UNEXPECTED_ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class InvoiceXmlParserTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 11, 25);
    private static final String EXTERNAL_CONTENT = "local-file-canary";
    private final InvoiceXmlParser parser = InvoiceXmlParser.defaults();

    @Test
    void parsesVatInvoiceAndPreservesDecimalScale() throws IOException {
        InvoiceDocument invoice = parser.parse(fixture("vat-two-rates.xml"));

        assertThat(invoice.header()).isEqualTo(new InvoiceHeader(
                "2.1.1", "Hóa đơn giá trị gia tăng", "1", "C26MAU", "00000001", ISSUE_DATE, 0, null,
                "USD", new BigDecimal("25000.00"), "Chuyển khoản", "MST-MAU-GP"));
        assertThat(invoice.seller()).isEqualTo(new InvoiceParty("Công ty Mẫu A", "MST-MAU-A", "Địa chỉ mẫu A", null));
        assertThat(invoice.buyer()).isEqualTo(
                new InvoiceParty("Công ty Mẫu B", "MST-MAU-B", "Địa chỉ mẫu B", "Người Mua Mẫu"));
        assertThat(invoice.items()).containsExactly(
                new InvoiceItem(1, "MAU-1", "Sổ mẫu", "quyển",
                        new BigDecimal("2.00"), new BigDecimal("10.50"), new BigDecimal("21.000"), null, null, "5%"),
                new InvoiceItem(1, "MAU-2", "Bút mẫu", "chiếc",
                        new BigDecimal("3"), new BigDecimal("4.00"), new BigDecimal("12.00"), null, null, "10%"),
                new InvoiceItem(1, "MAU-3", "Giấy mẫu", "tập",
                        new BigDecimal("1.0"), new BigDecimal("9.000"), new BigDecimal("9.00"), null, null, "5%"));
        assertThat(invoice.taxRateSummaries()).containsExactly(
                new TaxRateSummary("5%", new BigDecimal("30.000"), new BigDecimal("1.50")),
                new TaxRateSummary("10%", new BigDecimal("12.00"), new BigDecimal("1.200")));
        assertThat(invoice.totals()).isEqualTo(new InvoiceTotals(
                new BigDecimal("42.000"), new BigDecimal("2.700"), null, new BigDecimal("44.7000"),
                "Bốn mươi bốn đô la Mỹ và bảy mươi xu"));
        assertThat(invoice.taxAuthorityCode()).isEqualTo("MA-CQT-MAU");
        assertThat(invoice.extensions()).isEmpty();
        assertThat(invoice.signatures()).isEmpty();
    }

    @Test
    void usesLocalNamesAndIndexesAllRepeatedPathSteps() throws IOException {
        InvoiceDocument invoice = parser.parse(fixture("vat-with-extensions.xml"));

        assertIdentity(invoice, "00000002", "K26MAU", "Công ty Mẫu C");
        assertThat(invoice.items()).containsExactly(
                new InvoiceItem(1, null, "Dịch vụ mẫu thứ nhất", null, null, null,
                        new BigDecimal("1000.00"), null, new BigDecimal("100.00"), "10%"),
                new InvoiceItem(1, null, "Dịch vụ mẫu thứ hai", null, null, null,
                        new BigDecimal("2000.00"), null, new BigDecimal("200.00"), "10%"));
        assertThat(invoice.taxRateSummaries())
                .containsExactly(new TaxRateSummary("10%", new BigDecimal("3000.00"), new BigDecimal("300")));
        assertThat(invoice.totals()).isEqualTo(new InvoiceTotals(
                new BigDecimal("3000"), new BigDecimal("300"), null, new BigDecimal("3300"), "Ba nghìn ba trăm đồng"));
        assertThat(invoice.taxAuthorityCode()).isNull();
        assertThat(invoice.extensions()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "HDon/DLHDon/NDHDon/NBan/MDDKDoanh[1]/TDDKDoanh", "  Điểm mẫu 1  ",
                "HDon/DLHDon/NDHDon/NBan/MDDKDoanh[2]/TDDKDoanh", "Điểm mẫu 2"));
    }

    @Test
    void parsesDiscountsWithTheirOriginalDecimalScale() throws IOException {
        var invoice = parser.parse(fixture("vat-line-discount.xml"));

        assertThat(invoice.items().get(0).discountRate()).isEqualTo(new BigDecimal("10.000"));
        assertThat(invoice.items().get(0).discountAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(invoice.items().get(1).discountRate()).isNull();
        assertThat(invoice.items().get(1).discountAmount()).isNull();
        assertThat(invoice.totals().totalTradeDiscount()).isEqualTo(new BigDecimal("50.000"));
        assertThat(invoice.extensions()).isEmpty();
    }

    @Test
    void rejectsInvalidDiscountNumbersAtTheirXmlPaths() throws IOException {
        String xml = new String(fixture("vat-line-discount.xml"), StandardCharsets.UTF_8);
        for (String tag : List.of("TLCKhau", "STCKhau", "TTCKTMai")) {
            String invalid = xml.replaceFirst("<" + tag + ">[^<]+", "<" + tag + ">invalid");
            var error = assertThrows(InvoiceXmlException.class, () -> parser.parse(bytes(invalid)));
            String parent = tag.equals("TTCKTMai") ? "TToan" : "DSHHDVu/HHDVu[1]";
            assertThat(error.code()).isEqualTo(INVALID_NUMBER);
            assertThat(error.path()).isEqualTo("HDon/DLHDon/NDHDon/" + parent + "/" + tag);
        }
    }

    @Test
    void missingBuyerAndInvoiceNumberAreAllowed() throws IOException {
        InvoiceDocument invoice = parser.parse(fixture("sales-no-buyer.xml"));

        assertIdentity(invoice, null, "K26MAU", "Cửa hàng Mẫu D");
        assertThat(invoice.header().invoiceTemplateSymbol()).isEqualTo("2");
        assertThat(invoice.header().nonTariffAreaFlag()).isZero();
        assertThat(invoice.buyer()).isNull();
        assertThat(invoice.items()).containsExactly(
                new InvoiceItem(1, null, "Hàng mẫu", null, null, null, new BigDecimal("90"), null, null, null));
        assertThat(invoice.taxRateSummaries()).isEmpty();
        assertThat(invoice.totals()).isEqualTo(new InvoiceTotals(null, null, null, new BigDecimal("90"), "Chín mươi đồng"));
        assertThat(invoice.taxAuthorityCode()).isNull();
    }

    @Test
    void readsUtf16WithBomAndDeclaration() throws IOException {
        byte[] xml = fixture("utf16-encoded.xml");
        InvoiceDocument invoice = parser.parse(xml);

        assertThat(xml).startsWith((byte) 0xff, (byte) 0xfe);
        assertIdentity(invoice, "00000004", "K26MAU", "Cửa hàng Mẫu Đ");
        assertThat(invoice.items()).containsExactly(
                new InvoiceItem(1, null, "Ấm trà mẫu", null, null, null, new BigDecimal("70.00"), null, null, null));
        assertThat(invoice.taxRateSummaries()).isEmpty();
        assertThat(invoice.totals()).isEqualTo(
                new InvoiceTotals(null, null, null, new BigDecimal("70.00"), "Bảy mươi đồng"));
        assertThat(invoice.taxAuthorityCode()).isNull();
        assertThat(parser.parse(new ByteArrayInputStream(xml))).isEqualTo(invoice);
    }

    @Test
    void readsEncodingFromDeclarationWithoutBom() {
        byte[] xml = """
                <?xml version="1.0" encoding="ISO-8859-1"?>
                <HDon><DLHDon><NDHDon><NBan><Ten>Cà phê</Ten></NBan></NDHDon></DLHDon></HDon>
                """.getBytes(StandardCharsets.ISO_8859_1);

        assertThat(parser.parse(xml).seller().name()).isEqualTo("Cà phê");
    }

    @Test
    void readsSigningTimesAndPublicCertificatesFromSyntheticSignatures() throws IOException {
        InvoiceDocument invoice = parser.parse(fixture("signed-metadata.xml"));

        assertThat(invoice.signatures()).containsExactly(
                new InvoiceSignature(SELLER, Instant.parse("2026-11-25T02:30:00Z"),
                        certificate("Synthetic Test Seller", "8da396a3a0c0264b")),
                new InvoiceSignature(TAX_AUTHORITY, Instant.parse("2026-11-25T02:45:00Z"),
                        certificate("Synthetic Test Tax Authority", "155f060354d02830")));
        assertThat(invoice.extensions()).isEmpty();
    }

    @Test
    void readsBuyerMetadataAndUsesOnlyTheFirstXmlDsigCertificate() throws IOException {
        String xml = new String(fixture("signed-metadata.xml"), StandardCharsets.UTF_8)
                .replace("<NBan>\n      <ds:Signature>", "<NMua>\n      <ds:Signature>")
                .replace("</ds:Signature>\n    </NBan>", "</ds:Signature>\n    </NMua>")
                .replaceFirst("<ds:X509Data>", """
                        <ds:X509Data><xades:X509Certificate>a===</xades:X509Certificate>
                        """)
                .replaceFirst("</ds:X509Certificate>", """
                        </ds:X509Certificate><ds:X509Certificate>a===</ds:X509Certificate>
                        """);

        InvoiceSignature buyer = parser.parse(bytes(xml)).signatures().get(0);
        assertThat(buyer).isEqualTo(new InvoiceSignature(BUYER, Instant.parse("2026-11-25T02:30:00Z"),
                certificate("Synthetic Test Seller", "8da396a3a0c0264b")));
    }

    @Test
    void readsEveryXmlDsigSignatureAndAllowsMissingMetadata() {
        InvoiceDocument invoice = parse("""
                <HDon xmlns:d="http://www.w3.org/2000/09/xmldsig#" xmlns:other="urn:synthetic:other">
                  <DSCKS>
                    <NMua>
                      <other:Signature/>
                      <d:Signature>
                        <SigningTime>outside-object</SigningTime>
                        <other:Object><SigningTime>wrong-namespace</SigningTime></other:Object>
                        <d:Object><SigningTime> </SigningTime></d:Object>
                      </d:Signature>
                      <d:Signature><d:KeyInfo/></d:Signature>
                    </NMua>
                  </DSCKS>
                </HDon>
                """);

        assertThat(invoice.signatures()).containsExactly(
                new InvoiceSignature(BUYER, null, null), new InvoiceSignature(BUYER, null, null));
    }

    @Test
    void rejectsInvalidBase64AndNonCertificateData() {
        for (String invalidCertificate : List.of("a===", "RkFLRQ==")) {
            InvoiceXmlException error = failure("""
                    <HDon xmlns:d="http://www.w3.org/2000/09/xmldsig#">
                      <DSCKS><NBan><d:Signature><d:KeyInfo><d:X509Data>
                        <d:X509Certificate>%s</d:X509Certificate>
                      </d:X509Data></d:KeyInfo></d:Signature></NBan></DSCKS>
                    </HDon>
                    """.formatted(invalidCertificate), INVALID_CERTIFICATE,
                    "HDon/DSCKS/NBan/Signature/KeyInfo/X509Data/X509Certificate");
            assertThat(error).hasMessageNotContaining(invalidCertificate).hasNoCause();
        }
    }

    @Test
    void signingTimeRequiresAnIsoDateTimeWithOffset() {
        String invalidTime = "2026-11-25T09:30:00";
        InvoiceXmlException error = failure("""
                <HDon xmlns:d="http://www.w3.org/2000/09/xmldsig#">
                  <DSCKS><NBan><d:Signature><d:Object>
                    <SigningTime>%s</SigningTime>
                  </d:Object></d:Signature></NBan></DSCKS>
                </HDon>
                """.formatted(invalidTime), INVALID_DATE, "HDon/DSCKS/NBan/Signature/Object/SigningTime");
        assertThat(error).hasMessageNotContaining(invalidTime).hasNoCause();
    }

    @Test
    void rejectsRepeatedSigningTimeUnderTheSameParent() {
        failure("""
                <HDon xmlns:d="http://www.w3.org/2000/09/xmldsig#" xmlns:other="urn:synthetic:other">
                  <DSCKS><NBan><d:Signature><d:Object>
                    <SigningTime/><other:SigningTime/>
                  </d:Object></d:Signature></NBan></DSCKS>
                </HDon>
                """, DUPLICATE_ELEMENT, "HDon/DSCKS/NBan/Signature/Object/SigningTime[2]");
    }

    @Test
    void keepsStringWhitespaceAndTreatsBlankTypedValuesAsNull() {
        InvoiceDocument invoice = parse("""
                <HDon><DLHDon><TTChung>
                  <PBan> 9.9.9 </PBan><THDon/><SHDon>  </SHDon>
                  <NLap>  </NLap><HDCTTChinh/><TGia>  </TGia>
                </TTChung></DLHDon><MCCQT/></HDon>
                """);

        assertThat(invoice.header()).isEqualTo(new InvoiceHeader(
                " 9.9.9 ", "", null, null, "  ", null, null, null, null, null, null, null));
        assertThat(invoice.taxAuthorityCode()).isEmpty();
        assertThat(invoice.items()).isEmpty();
        assertThat(invoice.seller()).isNull();
        assertThat(invoice.totals()).isNull();
    }

    @Test
    void doesNotRejectAnUnknownVersion() {
        assertThat(parse("<HDon><DLHDon><TTChung><PBan>9.9.9</PBan></TTChung></DLHDon></HDon>")
                .header().formatVersion()).isEqualTo("9.9.9");
    }

    @Test
    void keepsOnlyUnmappedLeavesWithText() {
        InvoiceDocument invoice = parse("""
                <p:HDon xmlns:p="urn:synthetic:prefixed">
                  <TDDKDoanh>parent text<STCKhau><![CDATA[a&b]]></STCKhau><STCKhau>  </STCKhau></TDDKDoanh>
                  <MDDKDoanh/>
                </p:HDon>
                """);

        assertThat(invoice.extensions()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "HDon/TDDKDoanh/STCKhau[1]", "a&b",
                "HDon/TDDKDoanh/STCKhau[2]", "  "));
    }

    @Test
    void rejectsExternalEntityDeclarationWithoutDisclosingLocalFileContent(@TempDir Path temp) throws IOException {
        Path marker = Files.writeString(temp.resolve("marker.txt"), EXTERNAL_CONTENT);
        InvoiceXmlException error = failure("""
                <!DOCTYPE HDon [<!ENTITY sample SYSTEM "%s">]>
                <HDon/>
                """.formatted(marker.toUri()), DOCTYPE_NOT_ALLOWED, null);

        assertThat(error).hasMessageNotContaining(EXTERNAL_CONTENT).hasNoCause();
    }

    @Test
    void rejectsNestedEntityDeclarationsBeforeExpansion() {
        // A bounded sample of nested entities exercises the same prolog rejection as an expansion bomb.
        failure("""
                <!DOCTYPE HDon [
                  <!ENTITY first "sample">
                  <!ENTITY second "&first;&first;">
                  <!ENTITY third "&second;&second;">
                ]>
                <HDon><MCCQT>&third;</MCCQT></HDon>
                """, DOCTYPE_NOT_ALLOWED, null);
    }

    @Test
    void rejectsDoctypeWithoutEntities() {
        failure("<!DOCTYPE HDon [<!ELEMENT HDon EMPTY>]><HDon/>", DOCTYPE_NOT_ALLOWED, null);
    }

    @Test
    void rejectsExternalDtd(@TempDir Path temp) throws IOException {
        Path dtd = Files.writeString(temp.resolve("sample.dtd"), "<!ELEMENT HDon EMPTY>");
        failure("<!DOCTYPE HDon SYSTEM \"" + dtd.toUri() + "\"><HDon/>", DOCTYPE_NOT_ALLOWED, null);
    }

    @Test
    void doctypeTextInCommentsAndCdataIsNotADeclaration() {
        assertThat(parse("<HDon><!-- <!DOCTYPE HDon> --><MCCQT><![CDATA[<!DOCTYPE HDon>]]></MCCQT></HDon>")
                .taxAuthorityCode()).isEqualTo("<!DOCTYPE HDon>");
    }

    @Test
    void xincludeDoesNotReadExternalContent(@TempDir Path temp) throws IOException {
        Path marker = Files.writeString(temp.resolve("include.txt"), EXTERNAL_CONTENT);
        InvoiceDocument invoice = parse("""
                <HDon xmlns:xi="http://www.w3.org/2001/XInclude">
                  <MCCQT>safe<xi:include href="%s" parse="text"/></MCCQT>
                </HDon>
                """.formatted(marker.toUri()));

        assertThat(invoice.taxAuthorityCode()).isEqualTo("safe");
        assertThat(invoice.extensions()).isEmpty();
    }

    @Test
    void stopsReadingAnUnboundedStreamAtTheFirstByteBeyondTheLimit() {
        int limit = 128;
        InvoiceXmlParser limited = InvoiceXmlParser.builder().maxInputBytes(limit).build();
        RepeatingXmlStream stream = new RepeatingXmlStream();

        InvoiceXmlException error = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> assertThrows(InvoiceXmlException.class, () -> limited.parse(stream)));

        assertThat(error.code()).isEqualTo(INPUT_TOO_LARGE);
        assertThat(error.path()).isNull();
        assertThat(stream.bytesRead).isEqualTo(limit + 1);
    }

    @Test
    void rejectsOversizedArrayBeforeDecodingAndAllowsExactLimitForBothOverloads() {
        byte[] xml = bytes("<HDon/>");
        InvoiceXmlParser limited = InvoiceXmlParser.builder().maxInputBytes(xml.length).build();

        assertThat(limited.parse(xml)).isEqualTo(limited.parse(new ByteArrayInputStream(xml)));
        assertThat(assertThrows(InvoiceXmlException.class, () -> limited.parse(new byte[xml.length + 1])).code())
                .isEqualTo(INPUT_TOO_LARGE);
        assertThat(assertThrows(InvoiceXmlException.class, () -> limited.parse(bytes("<HDon/> "))).code())
                .isEqualTo(INPUT_TOO_LARGE);
        assertThat(assertThrows(InvoiceXmlException.class,
                () -> limited.parse(new ByteArrayInputStream(bytes("<HDon/> ")))).code())
                .isEqualTo(INPUT_TOO_LARGE);
        assertThatIllegalArgumentException().isThrownBy(() -> InvoiceXmlParser.builder().maxInputBytes(0));
    }

    @Test
    void elementLimitIncludesTheRootAndAllowsExactlyTheConfiguredCount() {
        InvoiceXmlParser limited = InvoiceXmlParser.builder().maxElements(50).build();
        String prefix = "<HDon sample=\"ignored\">" + "<a/>".repeat(49);

        assertThat(limited.parse(bytes(prefix + "</HDon>")).extensions()).isEmpty();
        InvoiceXmlException error = assertThrows(InvoiceXmlException.class,
                () -> limited.parse(bytes(prefix + "<a/></HDon>")));
        assertThat(error.code()).isEqualTo(TOO_MANY_ELEMENTS);
        assertThat(error.path()).isNull();
    }

    @Test
    void rejectsNonPositiveElementLimits() {
        assertThatIllegalArgumentException().isThrownBy(() -> InvoiceXmlParser.builder().maxElements(0));
        assertThatIllegalArgumentException().isThrownBy(() -> InvoiceXmlParser.builder().maxElements(-1));
    }

    @Test
    void rejectsManySmallExtensionLeavesUnderTheDefaultByteLimit() {
        byte[] xml = bytes("<HDon>" + "<a>1</a>".repeat(256_000) + "</HDon>");

        InvoiceXmlException error = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertThrows(InvoiceXmlException.class, () -> parser.parse(xml)));

        assertThat(error.code()).isEqualTo(TOO_MANY_ELEMENTS);
        assertThat(error.path()).isNull();
    }

    @Test
    void leavesTheStreamOpenAndSanitizesReadFailures() {
        InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("sensitive-source");
            }

            @Override
            public void close() {
                throw new AssertionError("The parser must leave the supplied input open");
            }
        };

        InvoiceXmlException error = assertThrows(InvoiceXmlException.class, () -> parser.parse(input));
        assertThat(error.code()).isEqualTo(IO_ERROR);
        assertThat(error).hasMessageNotContaining("sensitive-source").hasNoCause();
    }

    @Test
    void reportsInvalidNumberAtAnIndexedPathWithoutDisclosingItsValue() {
        InvoiceXmlException error = failure("""
                <HDon><DLHDon><NDHDon><DSHHDVu>
                  <HHDVu><ThTien>0</ThTien></HHDVu><HHDVu><ThTien>abc</ThTien></HHDVu>
                </DSHHDVu></NDHDon></DLHDon></HDon>
                """, INVALID_NUMBER, "HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[2]/ThTien");
        assertThat(error).hasMessageNotContaining("abc").hasNoCause();
    }

    @Test
    void reportsInvalidIntegerIncludingOverflow() {
        failure("<HDon><DLHDon><TTChung><HDCTTChinh>2147483648</HDCTTChinh></TTChung></DLHDon></HDon>",
                INVALID_INTEGER, "HDon/DLHDon/TTChung/HDCTTChinh");
    }

    @Test
    void requiresFourDigitIsoDateAndValidCalendarDate() {
        for (String invalidDate : List.of("25/11/2026", "+12026-11-25", "2026-02-30")) {
            failure("<HDon><DLHDon><TTChung><NLap>" + invalidDate + "</NLap></TTChung></DLHDon></HDon>",
                    INVALID_DATE, "HDon/DLHDon/TTChung/NLap");
        }
    }

    @Test
    void detectsDuplicateSingletonsByLocalName() {
        failure("""
                <HDon xmlns:p="urn:synthetic:prefixed"><DLHDon><TTChung>
                  <PBan>2.1.1</PBan><p:PBan>2.1.0</p:PBan>
                </TTChung></DLHDon></HDon>
                """, DUPLICATE_ELEMENT, "HDon/DLHDon/TTChung/PBan[2]");
        failure("<HDon><MCCQT/><MCCQT/></HDon>", DUPLICATE_ELEMENT, "HDon/MCCQT[2]");
    }

    @Test
    void reportsUnexpectedRootAndMalformedXml() {
        failure("<not-HDon><HDon/></not-HDon>", UNEXPECTED_ROOT, null);
        InvoiceXmlException error = failure("<HDon><sensitive-name></HDon>", MALFORMED_XML, null);
        assertThat(error).hasMessageNotContaining("sensitive-name").hasNoCause();
    }

    @Test
    void classifiesInvalidByteSequencesAsMalformedXml() {
        byte[] xml = "<HDon>Ã(</HDon>".getBytes(StandardCharsets.ISO_8859_1);
        InvoiceXmlException error = assertThrows(InvoiceXmlException.class, () -> parser.parse(xml));
        assertThat(error.code()).isEqualTo(MALFORMED_XML);
    }

    @Test
    void classifiesUnsupportedDeclaredEncodingAsMalformedXml() {
        failure("<?xml version=\"1.0\" encoding=\"UNSUPPORTED-ENCODING\"?><HDon/>", MALFORMED_XML, null);
    }

    @Test
    void sharesOneParserAcrossEightSimultaneousCalls() throws Exception {
        byte[] xml = fixture("vat-two-rates.xml");
        InvoiceDocument expected = parser.parse(xml);
        int workers = 8;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<InvoiceDocument>> results = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return parser.parse(new ByteArrayInputStream(xml));
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<InvoiceDocument> result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo(expected);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertIdentity(InvoiceDocument invoice, String number, String symbol, String sellerName) {
        assertThat(invoice.header().invoiceNumber()).isEqualTo(number);
        assertThat(invoice.header().invoiceSymbol()).isEqualTo(symbol);
        assertThat(invoice.header().issueDate()).isEqualTo(ISSUE_DATE);
        assertThat(invoice.seller().name()).isEqualTo(sellerName);
    }

    private static CertificateInfo certificate(String commonName, String serialNumber) {
        String dn = "CN=" + commonName;
        return new CertificateInfo(dn, dn, serialNumber, Instant.parse("2026-11-25T00:00:00Z"),
                Instant.parse("2036-11-25T00:00:00Z"));
    }

    private InvoiceDocument parse(String xml) {
        return parser.parse(bytes(xml));
    }

    private InvoiceXmlException failure(String xml, InvoiceXmlErrorCode code, String path) {
        InvoiceXmlException error = assertThrows(InvoiceXmlException.class, () -> parse(xml));
        assertThat(error.code()).isEqualTo(code);
        assertThat(error.path()).isEqualTo(path);
        return error;
    }

    private static byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] fixture(String name) throws IOException {
        try (InputStream resource = Objects.requireNonNull(
                InvoiceXmlParserTest.class.getResourceAsStream("/einvoice/synthetic/" + name), name)) {
            return resource.readAllBytes();
        }
    }

    private static final class RepeatingXmlStream extends InputStream {

        private final byte[] prefix = bytes("<HDon><MCCQT>");
        private int bytesRead;

        @Override
        public int read() throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Test interrupted");
            }
            int position = bytesRead++;
            return position < prefix.length ? prefix[position] : 'a';
        }
    }
}
