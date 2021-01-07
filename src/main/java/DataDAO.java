public class DataDAO {

    public static Company createCompany() {

        Company company = new Company();
        company.setId(111);
        company.setName("Microsoft");

        String[] websites = { "http://microsoft.com",
                "http://msn.com", "http://hotmail.com" };
        company.setWebsites(websites);

        Address address = new Address();
        address.setCity("Redmond");
        address.setStreet("1 Microsoft Way");

        company.setAddress(address);

        return company;
    }
}
