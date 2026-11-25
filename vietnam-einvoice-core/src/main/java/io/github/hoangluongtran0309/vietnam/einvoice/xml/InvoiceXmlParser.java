package io.github.hoangluongtran0309.vietnam.einvoice.xml;

import io.github.hoangluongtran0309.vietnam.einvoice.document.CertificateInfo;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceHeader;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceItem;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceParty;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceSignature;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceTotals;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.document.TaxRateSummary;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

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

/** An immutable, thread-safe parser for issued invoice XML. */
public final class InvoiceXmlParser {

    private static final long DEFAULT_MAX_INPUT_BYTES = 10L * 1024 * 1024;
    private static final int DEFAULT_MAX_ELEMENTS = 100_000;
    private static final Pattern ISSUE_DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final String XMLDSIG_NAMESPACE = "http://www.w3.org/2000/09/xmldsig#";

    private final long maxInputBytes;
    private final int maxElements;

    private InvoiceXmlParser(long maxInputBytes, int maxElements) {
        this.maxInputBytes = maxInputBytes;
        this.maxElements = maxElements;
    }

    /** @return a parser with a 10 MiB byte limit and a 100,000-element limit */
    public static InvoiceXmlParser defaults() {
        return new InvoiceXmlParser(DEFAULT_MAX_INPUT_BYTES, DEFAULT_MAX_ELEMENTS);
    }

    /** @return a builder with the default byte and element limits */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses a byte array using XML encoding detection.
     *
     * @param xml invoice XML bytes
     * @return data from the invoice
     * @throws InvoiceXmlException if the input exceeds a limit or cannot be parsed
     */
    public InvoiceDocument parse(byte[] xml) {
        Objects.requireNonNull(xml, "xml");
        if (xml.length > maxInputBytes) {
            throw new InvoiceXmlException(INPUT_TOO_LARGE, null);
        }
        return parse(new ByteArrayInputStream(xml));
    }

    /**
     * Parses a byte stream using XML encoding detection. The caller owns the stream and must close it.
     *
     * @param xml invoice XML input stream
     * @return data from the invoice
     * @throws InvoiceXmlException if the input exceeds a limit, cannot be read, or cannot be parsed
     */
    public InvoiceDocument parse(InputStream xml) {
        Objects.requireNonNull(xml, "xml");
        XmlTreeHandler handler = new XmlTreeHandler(maxElements);
        XMLReader reader = secureReader(handler);
        try {
            reader.parse(new InputSource(new BoundedInputStream(xml, maxInputBytes)));
        } catch (SAXException | IOException ex) {
            throw new InvoiceXmlException(MALFORMED_XML, null);
        }
        return mapDocument(handler.root);
    }

    private static XMLReader secureReader(XmlTreeHandler handler) {
        try {
            SAXParserFactory factory = SAXParserFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            return reader;
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IllegalStateException("Could not configure the secure JDK XML reader", ex);
        }
    }

    private static InvoiceDocument mapDocument(XmlElement root) {
        // TODO(v0.3): verify the tax-authority message wrapper — official Decision 1233/QD-CT.
        if (!root.name.equals("HDon")) {
            throw new InvoiceXmlException(UNEXPECTED_ROOT, null);
        }

        XmlElement data = single(root, "DLHDon");
        XmlElement header = single(data, "TTChung");
        XmlElement content = single(data, "NDHDon");
        XmlElement seller = single(content, "NBan");
        XmlElement buyer = single(content, "NMua");
        XmlElement items = single(content, "DSHHDVu");
        XmlElement totals = single(content, "TToan");
        XmlElement summaries = single(totals, "THTTLTSuat");

        return new InvoiceDocument(
                header == null ? null : mapHeader(header),
                seller == null ? null : mapParty(seller, false),
                buyer == null ? null : mapParty(buyer, true),
                mapChildren(items, "HHDVu", InvoiceXmlParser::mapItem),
                mapChildren(summaries, "LTSuat", InvoiceXmlParser::mapSummary),
                totals == null ? null : mapTotals(totals),
                string(root, "MCCQT"),
                signatures(root),
                extensions(root));
    }

    private static InvoiceHeader mapHeader(XmlElement header) {
        return new InvoiceHeader(
                string(header, "PBan"),
                string(header, "THDon"),
                string(header, "KHMSHDon"),
                string(header, "KHHDon"),
                string(header, "SHDon"),
                readValue(header, "NLap", InvoiceXmlParser::issueDate, INVALID_DATE),
                integer(header, "HDCTTChinh"),
                integer(header, "HDDCKPTQuan"),
                string(header, "DVTTe"),
                decimal(header, "TGia"),
                string(header, "HTTToan"),
                string(header, "MSTTCGP"));
    }

    private static InvoiceParty mapParty(XmlElement party, boolean buyer) {
        return new InvoiceParty(
                string(party, "Ten"),
                string(party, "MST"),
                string(party, "DChi"),
                buyer ? string(party, "HVTNMHang") : null);
    }

    private static InvoiceItem mapItem(XmlElement item) {
        return new InvoiceItem(
                integer(item, "TChat"),
                string(item, "MHHDVu"),
                string(item, "THHDVu"),
                string(item, "DVTinh"),
                decimal(item, "SLuong"),
                decimal(item, "DGia"),
                decimal(item, "ThTien"),
                string(item, "TSuat"));
    }

    private static TaxRateSummary mapSummary(XmlElement summary) {
        return new TaxRateSummary(string(summary, "TSuat"), decimal(summary, "ThTien"), decimal(summary, "TThue"));
    }

    private static InvoiceTotals mapTotals(XmlElement totals) {
        return new InvoiceTotals(
                decimal(totals, "TgTCThue"),
                decimal(totals, "TgTThue"),
                decimal(totals, "TgTTTBSo"),
                string(totals, "TgTTTBChu"));
    }

    private static XmlElement single(XmlElement parent, String name) {
        if (parent == null) {
            return null;
        }
        XmlElement match = null;
        for (XmlElement child : parent.children) {
            if (child.name.equals(name)) {
                if (match != null) {
                    throw new InvoiceXmlException(DUPLICATE_ELEMENT, child.path());
                }
                match = child;
                match.mapped = true;
            }
        }
        return match;
    }

    private static <T> List<T> mapChildren(XmlElement parent, String name, Function<XmlElement, T> mapper) {
        if (parent == null) {
            return List.of();
        }
        List<T> values = new ArrayList<>();
        for (XmlElement child : parent.children) {
            if (child.name.equals(name)) {
                child.mapped = true;
                values.add(mapper.apply(child));
            }
        }
        return values;
    }

    private static String string(XmlElement parent, String name) {
        XmlElement element = single(parent, name);
        return element == null ? null : element.text();
    }

    private static BigDecimal decimal(XmlElement parent, String name) {
        return readValue(parent, name, BigDecimal::new, INVALID_NUMBER);
    }

    private static Integer integer(XmlElement parent, String name) {
        return readValue(parent, name, Integer::valueOf, INVALID_INTEGER);
    }

    private static LocalDate issueDate(String text) {
        // TODO(v0.3): verify the NLap date format — official Decision 1233/QD-CT.
        if (!ISSUE_DATE.matcher(text).matches()) {
            throw new DateTimeParseException("Expected yyyy-MM-dd", text, 0);
        }
        return LocalDate.parse(text);
    }

    private static <T> T readValue(XmlElement parent, String name, Function<String, T> parse,
                                   InvoiceXmlErrorCode code) {
        return readValue(single(parent, name), parse, code);
    }

    private static <T> T readValue(XmlElement element, Function<String, T> parse, InvoiceXmlErrorCode code) {
        if (element == null) {
            return null;
        }
        String text = element.text().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return parse.apply(text);
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new InvoiceXmlException(code, element.path());
        }
    }

    private static List<InvoiceSignature> signatures(XmlElement root) {
        List<InvoiceSignature> signatures = new ArrayList<>();
        for (XmlElement child : root.children) {
            if (child.name.equals("DSCKS")) {
                collectSignatures(child, signatures);
            }
        }
        return signatures;
    }

    private static void collectSignatures(XmlElement container, List<InvoiceSignature> signatures) {
        for (XmlElement signer : container.children) {
            SignerRole role = switch (signer.name) {
                case "NBan" -> SignerRole.SELLER;
                case "NMua" -> SignerRole.BUYER;
                case "CQT" -> SignerRole.TAX_AUTHORITY;
                default -> null;
            };
            if (role == null) {
                continue;
            }
            for (XmlElement signature : signer.children) {
                if (signature.isXmlDsig("Signature")) {
                    Instant signingTime = readValue(signingTimeElement(signature),
                            text -> OffsetDateTime.parse(text).toInstant(), INVALID_DATE);
                    signatures.add(new InvoiceSignature(role, signingTime, certificate(signature)));
                }
            }
        }
    }

    private static XmlElement signingTimeElement(XmlElement signature) {
        // TODO(v0.3): verify the XAdES namespace and SigningTime path — official Decision 1233/QD-CT and XSD.
        XmlElement first = null;
        Deque<XmlElement> pending = new ArrayDeque<>();
        for (XmlElement object : signature.children) {
            if (!object.isXmlDsig("Object")) {
                continue;
            }
            pushChildren(pending, object);
            while (!pending.isEmpty()) {
                XmlElement element = pending.pop();
                if (element.name.equals("SigningTime")) {
                    if (element.index > 1) {
                        throw new InvoiceXmlException(DUPLICATE_ELEMENT, element.path());
                    }
                    if (first == null) {
                        first = element;
                    }
                }
                pushChildren(pending, element);
            }
        }
        return first;
    }

    private static void pushChildren(Deque<XmlElement> pending, XmlElement element) {
        for (int i = element.children.size() - 1; i >= 0; i--) {
            pending.push(element.children.get(i));
        }
    }

    private static CertificateInfo certificate(XmlElement signature) {
        for (XmlElement keyInfo : signature.children) {
            if (!keyInfo.isXmlDsig("KeyInfo")) {
                continue;
            }
            for (XmlElement data : keyInfo.children) {
                if (!data.isXmlDsig("X509Data")) {
                    continue;
                }
                for (XmlElement element : data.children) {
                    if (element.isXmlDsig("X509Certificate")) {
                        return readCertificate(element);
                    }
                }
            }
        }
        return null;
    }

    private static CertificateInfo readCertificate(XmlElement element) {
        try {
            byte[] bytes = Base64.getMimeDecoder().decode(element.text());
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
            return new CertificateInfo(
                    cert.getSubjectX500Principal().getName(),
                    cert.getIssuerX500Principal().getName(),
                    cert.getSerialNumber().toString(16),
                    cert.getNotBefore().toInstant(),
                    cert.getNotAfter().toInstant());
        } catch (IllegalArgumentException | CertificateException ex) {
            throw new InvoiceXmlException(INVALID_CERTIFICATE, element.path());
        }
    }

    private static Map<String, String> extensions(XmlElement root) {
        Map<String, String> values = new HashMap<>();
        Deque<XmlElement> pending = new ArrayDeque<>();
        for (XmlElement child : root.children) {
            if (!child.name.equals("DSCKS")) {
                pending.addLast(child);
            }
        }
        while (!pending.isEmpty()) {
            XmlElement element = pending.removeLast();
            if (!element.children.isEmpty()) {
                pending.addAll(element.children);
            } else if (!element.mapped && element.text != null) {
                values.put(element.path(), element.text());
            }
        }
        return values;
    }

    /** Configures the byte and element limits for a new parser. */
    public static final class Builder {

        private long maxInputBytes = DEFAULT_MAX_INPUT_BYTES;
        private int maxElements = DEFAULT_MAX_ELEMENTS;

        private Builder() {
        }

        /**
         * Sets the upper bound on XML bytes, including any BOM.
         *
         * @param maxInputBytes positive byte limit
         * @return this builder
         * @throws IllegalArgumentException if the limit is not positive
         */
        public Builder maxInputBytes(long maxInputBytes) {
            if (maxInputBytes <= 0) {
                throw new IllegalArgumentException("maxInputBytes must be positive");
            }
            this.maxInputBytes = maxInputBytes;
            return this;
        }

        /**
         * Sets the upper bound on XML elements, including the root element.
         *
         * @param maxElements positive element limit
         * @return this builder
         * @throws IllegalArgumentException if the limit is not positive
         */
        public Builder maxElements(int maxElements) {
            if (maxElements <= 0) {
                throw new IllegalArgumentException("maxElements must be positive");
            }
            this.maxElements = maxElements;
            return this;
        }

        /** @return a new thread-safe parser with the configured limits */
        public InvoiceXmlParser build() {
            return new InvoiceXmlParser(maxInputBytes, maxElements);
        }
    }

    private static final class XmlTreeHandler extends DefaultHandler2 {

        private int elementsRemaining;
        private XmlElement root;
        private XmlElement current;

        private XmlTreeHandler(int maxElements) {
            this.elementsRemaining = maxElements;
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) {
            // The lexical callback runs before the reader processes the DTD.
            throw new InvoiceXmlException(DOCTYPE_NOT_ALLOWED, null);
        }

        @Override
        public void startElement(String uri, String localName, String qualifiedName, Attributes attributes) {
            if (elementsRemaining == 0) {
                throw new InvoiceXmlException(TOO_MANY_ELEMENTS, null);
            }
            elementsRemaining--;
            // TODO(v0.3): verify business element namespaces — official Decision 1233/QD-CT and official XSD schema.
            XmlElement element = new XmlElement(localName, uri, current);
            if (current == null) {
                root = element;
            } else {
                current.addChild(element);
            }
            current = element;
        }

        @Override
        public void characters(char[] characters, int start, int length) {
            if (current != null && length > 0) {
                current.appendText(characters, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qualifiedName) {
            current.indexChildren();
            current = current.parent;
        }
    }

    private static final class XmlElement {

        private final String name;
        private final String namespace;
        private final XmlElement parent;
        private List<XmlElement> children = List.of();
        private StringBuilder text;
        private int index;
        private boolean mapped;

        private XmlElement(String name, String namespace, XmlElement parent) {
            this.name = name;
            this.namespace = namespace;
            this.parent = parent;
        }

        private boolean isXmlDsig(String localName) {
            return name.equals(localName) && XMLDSIG_NAMESPACE.equals(namespace);
        }

        private void addChild(XmlElement child) {
            if (children.isEmpty()) {
                children = new ArrayList<>();
            }
            children.add(child);
        }

        private void appendText(char[] characters, int start, int length) {
            if (text == null) {
                text = new StringBuilder(length);
            }
            text.append(characters, start, length);
        }

        private String text() {
            return text == null ? "" : text.toString();
        }

        private void indexChildren() {
            if (children.size() < 2) {
                return;
            }
            Map<String, Integer> siblingCounts = new HashMap<>();
            for (XmlElement child : children) {
                siblingCounts.merge(child.name, 1, Integer::sum);
            }
            Map<String, Integer> indexes = new HashMap<>();
            for (XmlElement child : children) {
                if (siblingCounts.get(child.name) > 1) {
                    child.index = indexes.merge(child.name, 1, Integer::sum);
                }
            }
        }

        private String path() {
            Deque<String> steps = new ArrayDeque<>();
            for (XmlElement element = this; element != null; element = element.parent) {
                String step = element.name;
                if (element.index != 0) {
                    step += "[" + element.index + "]";
                }
                steps.addFirst(step);
            }
            return String.join("/", steps);
        }
    }

    private static final class BoundedInputStream extends InputStream {

        private final InputStream input;
        private long remaining;

        private BoundedInputStream(InputStream input, long limit) {
            this.input = input;
            this.remaining = limit;
        }

        @Override
        public int read() {
            int next;
            try {
                next = input.read();
            } catch (IOException ex) {
                throw new InvoiceXmlException(IO_ERROR, null);
            }
            if (next >= 0) {
                count(1);
            }
            return next;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            // Read at most one byte beyond the limit to distinguish EOF from oversized input.
            int requested = remaining < length ? (int) remaining + 1 : length;
            int bytesRead;
            try {
                bytesRead = input.read(bytes, offset, requested);
            } catch (IOException ex) {
                throw new InvoiceXmlException(IO_ERROR, null);
            }
            if (bytesRead > 0) {
                count(bytesRead);
            }
            return bytesRead;
        }

        private void count(int count) {
            if (count > remaining) {
                throw new InvoiceXmlException(INPUT_TOO_LARGE, null);
            }
            remaining -= count;
        }
    }
}
