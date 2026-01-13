package signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for XML Document operations.
 * Provides methods for creating, parsing, and converting XML documents.
 */
public class DocumentUtil {

    private static final Logger logger = LoggerFactory.getLogger(DocumentUtil.class);

    private DocumentUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts an XML Document to a formatted string.
     *
     * @param doc the XML document to convert
     * @return the XML as a formatted string
     * @throws TransformerException if transformation fails
     */
    public static String asString(Document doc) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Creates a new empty XML Document.
     *
     * @return a new Document instance
     * @throws ParserConfigurationException if document builder cannot be created
     */
    public static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        logger.debug("Created new XML document");
        return builder.newDocument();
    }

    /**
     * Parses an XML file into a Document.
     *
     * @param xmlFilePath path to the XML file
     * @return the parsed Document
     * @throws ParserConfigurationException if document builder cannot be created
     * @throws SAXException if XML parsing fails
     * @throws IOException if file cannot be read
     */
    public static Document getXmlDocument(String xmlFilePath) throws ParserConfigurationException, SAXException, IOException {
        return getXmlDocument(Path.of(xmlFilePath));
    }

    /**
     * Parses an XML file into a Document.
     *
     * @param xmlFilePath path to the XML file
     * @return the parsed Document
     * @throws ParserConfigurationException if document builder cannot be created
     * @throws SAXException if XML parsing fails
     * @throws IOException if file cannot be read
     */
    public static Document getXmlDocument(Path xmlFilePath) throws ParserConfigurationException, SAXException, IOException {
        logger.debug("Parsing XML document from {}", xmlFilePath);
        try (InputStream inputStream = Files.newInputStream(xmlFilePath)) {
            return parseXmlDocument(inputStream);
        }
    }

    /**
     * Parses an XML InputStream into a Document.
     *
     * @param inputStream the input stream containing XML data
     * @return the parsed Document
     * @throws ParserConfigurationException if document builder cannot be created
     * @throws SAXException if XML parsing fails
     * @throws IOException if stream cannot be read
     */
    public static Document parseXmlDocument(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(inputStream);
    }
}
