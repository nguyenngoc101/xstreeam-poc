package model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.util.Arrays;

/**
 * Represents a company entity with basic information.
 * Uses XStream annotations for XML serialization.
 */
@XStreamAlias("company")
public class Company {

    @XStreamAlias("id")
    @XStreamAsAttribute
    private int id;

    private String name;
    private String[] websites;
    private Address address;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getWebsites() {
        return websites;
    }

    public void setWebsites(String[] websites) {
        this.websites = websites;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Company{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        if (websites != null) {
            sb.append(", websites=").append(Arrays.toString(websites));
        }
        if (address != null) {
            sb.append(", address=").append(address);
        }
        sb.append('}');
        return sb.toString();
    }
}
