import com.alibaba.fastjson.JSONObject;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

public class JavaObject2Xml {

    public static void main(String[] args) {

        Company company = DataDAO.createCompany();

        XStream xs = new XStream();
        xs.processAnnotations(Company.class);
//        xs.alias("company", Company.class);
//        xs.aliasField("companyName", Company.class, "name");

        // JAVA OBJECT --> XML
        // xml -> java -> json
        // json <-> java <-> xml
        String xml = xs.toXML(company);
        String xmlStr = "<company id=\"111\">\n" +
                "  <name>Microsoft</name>\n" +
                "  <websites>\n" +
                "    <string>http://microsoft.com</string>\n" +
                "    <string>http://msn.com</string>\n" +
                "    <string>http://hotmail.com</string>\n" +
                "  </websites>\n" +
                "  <address city=\"Ha Noi\">Cau Giay</address>\n" +
                "</company>\n";


        Company company1 = (Company)xs.fromXML(xmlStr);

//        System.out.println(company1);
//
//        System.out.println(xs.toXML(company1));

        Object o = JSONObject.toJSON(company1);
        System.out.println(o.toString());


        XStream xstream = new XStream(new JettisonMappedXmlDriver());
        xstream.processAnnotations(Company.class);
        String json = xstream.toXML(company1);
        System.out.println(json);

        Company company2 = (Company) xstream.fromXML(json);
        System.out.println(company2);

    }
}
