package demo;

import model.Address;
import model.Company;

/**
 * Factory class for creating test data objects.
 * Used for demonstrations and testing purposes.
 */
public class TestDataFactory {

    private TestDataFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a sample Company object with test data.
     *
     * @return a Company instance with sample data
     */
    public static Company createCompany() {
        Company company = new Company();
        company.setId(111);
        company.setName("Microsoft");
        company.setWebsites(new String[]{
                "http://microsoft.com",
                "http://msn.com",
                "http://hotmail.com"
        });

        Address address = new Address("1 Microsoft Way", "Redmond");
        company.setAddress(address);

        return company;
    }
}
