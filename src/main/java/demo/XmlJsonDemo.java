package demo;

import com.alibaba.fastjson2.JSON;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.security.NoTypePermission;
import model.Address;
import model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates XML and JSON serialization/deserialization using XStream.
 * Shows bidirectional conversion: Java Object <-> XML <-> JSON
 */
public class XmlJsonDemo {

    private static final Logger logger = LoggerFactory.getLogger(XmlJsonDemo.class);

    public static void main(String[] args) {
        XmlJsonDemo demo = new XmlJsonDemo();
        demo.runDemo();
    }

    public void runDemo() {
        logger.info("=== XStream XML/JSON Demo ===\n");

        // Create test data
        Company company = TestDataFactory.createCompany();
        logger.info("Original Company object:\n{}\n", company);

        // Java Object -> XML
        String xml = convertToXml(company);
        logger.info("Java -> XML:\n{}", xml);

        // XML -> Java Object
        Company fromXml = convertFromXml(xml);
        logger.info("XML -> Java:\n{}\n", fromXml);

        // Java Object -> JSON (using FastJSON)
        String json = convertToJson(fromXml);
        logger.info("Java -> JSON (FastJSON):\n{}\n", json);

        // Java Object -> JSON (using XStream + Jettison)
        String jettison = convertToJettisonJson(company);
        logger.info("Java -> JSON (Jettison):\n{}\n", jettison);

        // JSON -> Java Object (using XStream + Jettison)
        Company fromJson = convertFromJettisonJson(jettison);
        logger.info("JSON -> Java:\n{}", fromJson);
    }

    /**
     * Converts a Company object to XML using XStream.
     */
    public String convertToXml(Company company) {
        XStream xstream = createXStream();
        return xstream.toXML(company);
    }

    /**
     * Converts XML string to Company object using XStream.
     */
    public Company convertFromXml(String xml) {
        XStream xstream = createXStream();
        return (Company) xstream.fromXML(xml);
    }

    /**
     * Converts a Company object to JSON using FastJSON.
     */
    public String convertToJson(Company company) {
        return JSON.toJSONString(company);
    }

    /**
     * Converts a Company object to JSON using XStream with Jettison driver.
     */
    public String convertToJettisonJson(Company company) {
        XStream xstream = new XStream(new JettisonMappedXmlDriver());
        configureXStream(xstream);
        return xstream.toXML(company);
    }

    /**
     * Converts Jettison JSON string to Company object.
     */
    public Company convertFromJettisonJson(String json) {
        XStream xstream = new XStream(new JettisonMappedXmlDriver());
        configureXStream(xstream);
        return (Company) xstream.fromXML(json);
    }

    private XStream createXStream() {
        XStream xstream = new XStream();
        configureXStream(xstream);
        return xstream;
    }

    private void configureXStream(XStream xstream) {
        xstream.processAnnotations(Company.class);
        xstream.processAnnotations(Address.class);
        // Security: Use explicit type whitelist to prevent RCE attacks
        xstream.addPermission(NoTypePermission.NONE);
        xstream.allowTypes(new Class<?>[] { Company.class, Address.class });
        xstream.allowTypesByWildcard(new String[] { "java.lang.*", "java.util.*" });
    }
}
