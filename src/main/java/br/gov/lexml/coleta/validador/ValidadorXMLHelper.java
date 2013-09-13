
package br.gov.lexml.coleta.validador;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ValidadorXMLHelper {

    private static final Logger log = LoggerFactory.getLogger(ValidadorXMLHelper.class);

    private DocumentBuilder documentBuilder;

    private XPath xpath;

    private List<String> parseErrors = new ArrayList<String>();

    public ValidadorXMLHelper() {

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(true);
            dbf.setFeature("http://xml.org/sax/features/validation", true);
            dbf.setFeature("http://apache.org/xml/features/validation/schema", true);
            documentBuilder = dbf.newDocumentBuilder();
            documentBuilder.setErrorHandler(new MyErrorHandler());
            documentBuilder.setEntityResolver(new MyEntityResolver());
        }
        catch (ParserConfigurationException e) {
            log.error("Falha ao obter DocumentBuilder.", e);
            throw new RuntimeException(e);
        }

        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

            // O Xalan só utiliza este método
            public String getNamespaceURI(final String prefix) {
                return "http://www.lexml.gov.br/oai_lexml";
            }

            public String getPrefix(final String namespaceURI) {
                return null;
            }

            public Iterator getPrefixes(final String namespaceURI) {
                return null;
            }

        });

    }

    public Document parse(final InputStream is) throws SAXException, IOException {
        parseErrors.clear();
        return documentBuilder.parse(is);
    }

    public String getAttribute(final Node node, final String attrName) {
        Node attr = node.getAttributes().getNamedItem(attrName);
        if (attr != null) {
            return attr.getTextContent();
        }
        return null;
    }

    public Integer getAttributeAsInteger(final Node node, final String attrName) {
        String val = getAttribute(node, attrName);
        if (!StringUtils.isEmpty(val)) {
            return Integer.parseInt(val);
        }
        return null;
    }

    public String getString(final Node node, final String expression) {
        try {
            return xpath.evaluate(expression, node);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public NodeList getNodeList(final Node node, final String expression) {
        try {
            return (NodeList) xpath.evaluate(expression, node, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public String getParseErrorsAsString() {
        if (parseErrors.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String error : parseErrors) {
            sb.append(error);
            sb.append("\n");
        }
        return sb.toString();
    }

    public class MyErrorHandler implements ErrorHandler {

        public void error(final SAXParseException exception) throws SAXException {
            parseErrors.add(exception.getMessage());
        }

        public void fatalError(final SAXParseException exception) throws SAXException {
            error(exception);
        }

        public void warning(final SAXParseException exception) throws SAXException {
            error(exception);
        }

    }

    public class MyEntityResolver implements EntityResolver {

        private Pattern PATTERN = Pattern.compile("([^/]+\\.xsd)$", Pattern.CASE_INSENSITIVE);
        private EntityResolver defaultHandler = new DefaultHandler();

        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException,
                                                                                      IOException {

            Matcher m = PATTERN.matcher(systemId);
            if (m.find()) {
                String resourceName = "/xsd/" + m.group(1);
                InputStream is = getClass().getResourceAsStream(resourceName);
                if(is == null) {
                	log.warn("Schema não encontrado: " + resourceName);
                }
                else {
                	return new InputSource(is);
                }
            }

            return defaultHandler.resolveEntity(publicId, systemId);
        }

    }

}
