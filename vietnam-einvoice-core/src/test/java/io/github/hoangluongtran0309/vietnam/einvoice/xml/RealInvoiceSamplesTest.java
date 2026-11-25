package io.github.hoangluongtran0309.vietnam.einvoice.xml;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceParty;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/** Checks local samples on demand because real invoices contain personal data and are never committed. */
@EnabledIfEnvironmentVariable(named = "EINVOICE_REAL_SAMPLES_DIR", matches = ".+")
class RealInvoiceSamplesTest {

    private static final String HEADER = "HDon/DLHDon/TTChung";
    private static final String CONTENT = "HDon/DLHDon/NDHDon";
    private static final String ITEM = CONTENT + "/DSHHDVu/HHDVu";
    private static final String TOTALS = CONTENT + "/TToan";
    private static final String SUMMARY = TOTALS + "/THTTLTSuat/LTSuat";
    private static final Pattern PATH_INDEX = Pattern.compile("\\[\\d+\\]");

    @TestFactory
    Stream<DynamicTest> realInvoiceSamples() {
        List<Path> samples;
        try (var files = Files.list(Path.of(System.getenv("EINVOICE_REAL_SAMPLES_DIR")))) {
            samples = files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .toList();
        } catch (IOException | RuntimeException ex) {
            return fail("Could not list the XML files in EINVOICE_REAL_SAMPLES_DIR");
        }
        assertFalse(samples.isEmpty(), "EINVOICE_REAL_SAMPLES_DIR contains no XML files");
        return IntStream.range(0, samples.size()).mapToObj(index -> {
            String name = "sample-" + (index + 1);
            return dynamicTest(name, () -> checkSample(name, samples.get(index)));
        });
    }

    private static void checkSample(String name, Path sample) {
        var invoice = parseSample(sample);
        var leaves = leafPaths(readDom(sample));
        var mapped = mappedFields(invoice);
        var extensions = new TreeMap<String, Integer>();
        invoice.extensions().keySet().forEach(path -> extensions.merge(normalize(path), 1, Integer::sum));

        int leafCount = leaves.values().stream().mapToInt(Integer::intValue).sum();
        int mappedCount = mapped.counts.values().stream().mapToInt(Integer::intValue).sum();
        int accountedFor = mappedCount + invoice.extensions().size();
        System.out.printf("%s: DOM leaves=%d, mapped fields=%d, extensions=%d, maximum decimal scale=%s%n",
                name, leafCount, mappedCount, invoice.extensions().size(),
                mapped.decimalScales.values().stream().max(Integer::compareTo).map(Object::toString).orElse("none"));
        extensions.forEach((path, count) -> System.out.printf("%s: %s = %d%n", name, path, count));

        var header = invoice.header();
        assertAll(
                () -> assertTrue(header != null, "Invoice header is absent"),
                () -> assertTrue(header != null && header.formatVersion() != null, "Format version is absent"),
                () -> assertFalse(invoice.items().isEmpty(), "Invoice items are absent"),
                () -> mapped.decimalScales.forEach((path, scale) ->
                        assertTrue(scale <= 6, () -> "Decimal scale exceeds 6 at " + path)),
                () -> assertTrue(leafCount == accountedFor, () -> countMismatch(leaves, mapped.counts, extensions,
                        leafCount, accountedFor)),
                () -> invoice.extensions().keySet().forEach(path -> assertTrue(
                        path.startsWith("HDon/") && !path.contains("DSCKS"),
                        () -> "Invalid extension path: " + normalize(path))));
    }

    private static InvoiceDocument parseSample(Path sample) {
        try (var input = Files.newInputStream(sample)) {
            return InvoiceXmlParser.defaults().parse(input);
        } catch (InvoiceXmlException ex) {
            String path = ex.path() == null ? "" : " at " + normalize(ex.path());
            return fail("Invoice parser failed with " + ex.code() + path);
        } catch (IOException | RuntimeException ex) {
            return fail("Could not read or parse the sample with InvoiceXmlParser");
        }
    }

    private static Element readDom(Path sample) {
        try (var input = Files.newInputStream(sample)) {
            var factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            // Avoid the default error handler printing XML diagnostics to stderr.
            builder.setErrorHandler(new DefaultHandler());
            return builder.parse(input).getDocumentElement();
        } catch (IOException | ParserConfigurationException | SAXException | RuntimeException ex) {
            return fail("Could not read the sample with the secure DOM parser");
        }
    }

    private static Map<String, Integer> leafPaths(Element root) {
        assertTrue("HDon".equals(root.getLocalName()), "DOM root is not HDon");
        Map<String, Integer> leaves = new HashMap<>();
        Deque<ElementAtPath> pending = new ArrayDeque<>();
        pending.push(new ElementAtPath(root, "HDon"));
        while (!pending.isEmpty()) {
            var current = pending.pop();
            List<Element> children = new ArrayList<>();
            Map<String, Integer> siblingCounts = new HashMap<>();
            for (var node = current.element().getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element child) {
                    children.add(child);
                    siblingCounts.merge(child.getLocalName(), 1, Integer::sum);
                }
            }
            if (children.isEmpty() && current.element() != root
                    && !current.element().getTextContent().trim().isEmpty()) {
                leaves.merge(current.path(), 1, Integer::sum);
            }
            for (Element child : children) {
                String tag = child.getLocalName();
                if (current.element() == root && tag.equals("DSCKS")) {
                    continue;
                }
                String path = repeatedPath(current.path() + "/" + tag, siblingCounts.get(tag));
                pending.push(new ElementAtPath(child, path));
            }
        }
        return leaves;
    }

    private static MappedFields mappedFields(InvoiceDocument invoice) {
        var fields = new MappedFields();
        var header = invoice.header();
        if (header != null) {
            fields.add(HEADER, "PBan", header.formatVersion());
            fields.add(HEADER, "THDon", header.invoiceName());
            fields.add(HEADER, "KHMSHDon", header.invoiceTemplateSymbol());
            fields.add(HEADER, "KHHDon", header.invoiceSymbol());
            fields.add(HEADER, "SHDon", header.invoiceNumber());
            fields.add(HEADER, "NLap", header.issueDate());
            fields.add(HEADER, "HDCTTChinh", header.financialLeasingFlag());
            fields.add(HEADER, "HDDCKPTQuan", header.nonTariffAreaFlag());
            fields.add(HEADER, "DVTTe", header.currencyCode());
            fields.add(HEADER, "TGia", header.exchangeRate());
            fields.add(HEADER, "HTTToan", header.paymentMethod());
            fields.add(HEADER, "MSTTCGP", header.solutionProviderTaxCode());
        }
        putParty(fields, CONTENT + "/NBan", invoice.seller());
        putParty(fields, CONTENT + "/NMua", invoice.buyer());
        String itemPath = repeatedPath(ITEM, invoice.items().size());
        for (var item : invoice.items()) {
            fields.add(itemPath, "TChat", item.nature());
            fields.add(itemPath, "MHHDVu", item.itemCode());
            fields.add(itemPath, "THHDVu", item.name());
            fields.add(itemPath, "DVTinh", item.unit());
            fields.add(itemPath, "SLuong", item.quantity());
            fields.add(itemPath, "DGia", item.unitPrice());
            fields.add(itemPath, "ThTien", item.amount());
            fields.add(itemPath, "TSuat", item.taxRate());
        }
        String summaryPath = repeatedPath(SUMMARY, invoice.taxRateSummaries().size());
        for (var summary : invoice.taxRateSummaries()) {
            fields.add(summaryPath, "TSuat", summary.taxRate());
            fields.add(summaryPath, "ThTien", summary.amount());
            fields.add(summaryPath, "TThue", summary.taxAmount());
        }
        var totals = invoice.totals();
        if (totals != null) {
            fields.add(TOTALS, "TgTCThue", totals.totalAmountBeforeTax());
            fields.add(TOTALS, "TgTThue", totals.totalTaxAmount());
            fields.add(TOTALS, "TgTTTBSo", totals.totalPaymentAmount());
            fields.add(TOTALS, "TgTTTBChu", totals.totalPaymentAmountInWords());
        }
        fields.add("HDon", "MCCQT", invoice.taxAuthorityCode());
        return fields;
    }

    private static void putParty(MappedFields fields, String path, InvoiceParty party) {
        if (party != null) {
            fields.add(path, "Ten", party.name());
            fields.add(path, "MST", party.taxCode());
            fields.add(path, "DChi", party.address());
            fields.add(path, "HVTNMHang", party.buyerFullName());
        }
    }

    private static String repeatedPath(String path, int siblingCount) {
        return siblingCount > 1 ? path + "[n]" : path;
    }

    private static String normalize(String path) {
        return PATH_INDEX.matcher(path).replaceAll("[n]");
    }

    private static String countMismatch(Map<String, Integer> leaves, Map<String, Integer> mapped,
                                        Map<String, Integer> extensions, int leafCount, int accountedFor) {
        var difference = new HashMap<>(leaves);
        mapped.forEach((path, count) -> difference.merge(path, -count, Integer::sum));
        extensions.forEach((path, count) -> difference.merge(path, -count, Integer::sum));
        var paths = new TreeSet<String>();
        difference.forEach((path, count) -> {
            if (count != 0) {
                paths.add(path);
            }
        });
        return "DOM leaves: " + leafCount + ", mapped fields plus extensions: " + accountedFor
                + ", paths: " + paths;
    }

    private static final class MappedFields {
        private final Map<String, Integer> counts = new HashMap<>();
        private final Map<String, Integer> decimalScales = new TreeMap<>();

        private void add(String parent, String tag, Object value) {
            String path = parent + "/" + tag;
            if (value != null) {
                counts.merge(path, 1, Integer::sum);
            }
            if (value instanceof BigDecimal decimal) {
                decimalScales.merge(path, decimal.scale(), Math::max);
            }
        }
    }

    private record ElementAtPath(Element element, String path) {
    }
}
