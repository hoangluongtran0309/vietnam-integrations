package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.DOCTYPE_NOT_ALLOWED;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.DUPLICATE_ID;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.MALFORMED_XML;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.TOO_MANY_ELEMENTS;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.UNEXPECTED_ROOT;

final class SignatureXml {

    private SignatureXml() {
    }

    static Document read(byte[] xml, int maxElements) {
        checkElements(xml, maxElements);
        try {
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
            builder.setErrorHandler(new DefaultHandler());
            var document = builder.parse(new ByteArrayInputStream(xml));
            if (!"HDon".equals(document.getDocumentElement().getLocalName())) {
                throw new InvoiceXmlException(UNEXPECTED_ROOT, null);
            }
            return document;
        } catch (SAXException | IOException ex) {
            throw new InvoiceXmlException(MALFORMED_XML, null);
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException("Could not configure the secure JDK DOM parser", ex);
        }
    }

    private static void checkElements(byte[] xml, int maxElements) {
        var handler = new DefaultHandler2() {
            private int remaining = maxElements;

            @Override
            public void startDTD(String name, String publicId, String systemId) {
                // The lexical callback runs before the reader processes the DTD.
                throw new InvoiceXmlException(DOCTYPE_NOT_ALLOWED, null);
            }

            @Override
            public void startElement(String uri, String localName, String qualifiedName, Attributes attributes) {
                if (remaining == 0) {
                    throw new InvoiceXmlException(TOO_MANY_ELEMENTS, null);
                }
                remaining--;
            }
        };

        XMLReader reader;
        try {
            var factory = SAXParserFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            reader = factory.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IllegalStateException("Could not configure the secure JDK SAX parser", ex);
        }
        try {
            reader.parse(new InputSource(new ByteArrayInputStream(xml)));
        } catch (SAXException | IOException ex) {
            throw new InvoiceXmlException(MALFORMED_XML, null);
        }
    }

    static Map<String, Element> registerIds(Element root) {
        // TODO(v0.3): verify whether providers use other ID attribute names (ID, id) — real signed invoices.
        Map<String, Element> ids = new HashMap<>();
        var pending = new ArrayDeque<Element>();
        pending.push(root);
        while (!pending.isEmpty()) {
            Element element = pending.pop();
            var attribute = element.getAttributeNodeNS(null, "Id");
            if (attribute != null) {
                if (ids.putIfAbsent(attribute.getValue(), element) != null) {
                    throw new InvoiceXmlException(DUPLICATE_ID, null);
                }
                element.setIdAttributeNode(attribute, true);
            }
            for (Node node = element.getLastChild(); node != null; node = node.getPreviousSibling()) {
                if (node instanceof Element child) {
                    pending.push(child);
                }
            }
        }
        return ids;
    }

    static List<LocatedSignature> signatures(Element root) {
        var signatures = new ArrayList<LocatedSignature>();
        for (var container : indexedChildren(root, "HDon")) {
            if (!"DSCKS".equals(container.element().getLocalName())) {
                continue;
            }
            for (var signer : indexedChildren(container.element(), container.path())) {
                SignerRole role = switch (signer.element().getLocalName()) {
                    case "NBan" -> SignerRole.SELLER;
                    case "NMua" -> SignerRole.BUYER;
                    case "CQT" -> SignerRole.TAX_AUTHORITY;
                    default -> null;
                };
                if (role != null) {
                    for (var signature : indexedChildren(signer.element(), signer.path())) {
                        if (isDsig(signature.element(), "Signature")) {
                            signatures.add(new LocatedSignature(signature.element(), signature.path(), role));
                        }
                    }
                }
            }
        }
        return signatures;
    }

    static Element uniqueChild(Element parent, String name) {
        Element match = null;
        for (Element child : children(parent)) {
            if (name.equals(child.getLocalName())) {
                if (match != null) {
                    return null;
                }
                match = child;
            }
        }
        return match;
    }

    static Instant signingTime(Element signature) {
        Element first = null;
        var pending = new ArrayDeque<Element>();
        for (Element object : dsigChildren(signature, "Object")) {
            pushChildren(pending, object);
            while (!pending.isEmpty()) {
                Element element = pending.pop();
                if ("SigningTime".equals(element.getLocalName())) {
                    for (Node node = element.getPreviousSibling(); node != null; node = node.getPreviousSibling()) {
                        if (node instanceof Element sibling && "SigningTime".equals(sibling.getLocalName())) {
                            throw new IllegalArgumentException("Repeated SigningTime");
                        }
                    }
                    if (first == null) {
                        first = element;
                    }
                }
                pushChildren(pending, element);
            }
        }
        if (first == null) {
            return null;
        }
        var text = new StringBuilder();
        for (Node node = first.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(node.getNodeValue());
            }
        }
        String value = text.toString().trim();
        return value.isEmpty() ? null : OffsetDateTime.parse(value).toInstant();
    }

    static List<Element> dsigChildren(Element parent, String name) {
        return children(parent).stream().filter(child -> isDsig(child, name)).toList();
    }

    static Element requiredDsigChild(Element parent, String name) {
        List<Element> found = dsigChildren(parent, name);
        if (found.size() != 1) {
            throw new IllegalArgumentException("Missing or repeated XMLDSig element");
        }
        return found.get(0);
    }

    private static boolean isDsig(Element element, String name) {
        return name.equals(element.getLocalName()) && XMLSignature.XMLNS.equals(element.getNamespaceURI());
    }

    private static List<Element> children(Element parent) {
        var children = new ArrayList<Element>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element child) {
                children.add(child);
            }
        }
        return children;
    }

    private static void pushChildren(ArrayDeque<Element> pending, Element element) {
        for (Node node = element.getLastChild(); node != null; node = node.getPreviousSibling()) {
            if (node instanceof Element child) {
                pending.push(child);
            }
        }
    }

    private static List<ElementAtPath> indexedChildren(Element parent, String path) {
        var children = children(parent);
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Integer> indices = new HashMap<>();
        for (Element child : children) {
            counts.merge(child.getLocalName(), 1, Integer::sum);
        }
        var indexed = new ArrayList<ElementAtPath>(children.size());
        for (Element child : children) {
            String name = child.getLocalName();
            String step = counts.get(name) == 1 ? name : name + "[" + indices.merge(name, 1, Integer::sum) + "]";
            indexed.add(new ElementAtPath(child, path + "/" + step));
        }
        return indexed;
    }

    record LocatedSignature(Element element, String path, SignerRole role) {
    }

    private record ElementAtPath(Element element, String path) {
    }
}
