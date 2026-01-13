# XStream POC - XML Digital Signature & Serialization

A Java proof-of-concept demonstrating XML digital signatures and XStream serialization/deserialization.

## Features

- **XML Digital Signatures**: Sign and validate XML documents using RSA-SHA256
- **XStream Serialization**: Convert Java objects to/from XML
- **JSON Conversion**: Transform between XML, JSON, and Java objects
- **Key Management**: Generate, store, and load RSA key pairs

## Requirements

- Java 17+
- Maven 3.6+

## Quick Start

### Build
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Run Demo
```bash
mvn exec:java -Dexec.mainClass="demo.XmlJsonDemo"
```

## Project Structure

```
src/main/java/
├── demo/
│   ├── TestDataFactory.java    # Factory for test data objects
│   └── XmlJsonDemo.java        # XML/JSON conversion demo
├── model/
│   ├── Address.java            # Address model with XStream annotations
│   └── Company.java            # Company model with XStream annotations
└── signature/
    ├── DocumentUtil.java       # XML document utilities
    ├── KryptoUtil.java         # RSA key generation and storage
    └── XMLSignatureUtil.java   # XML digital signature operations

src/test/
├── java/signature/
│   └── XMLSignatureUtilTest.java
└── resources/
    └── purchase.xml            # Sample XML for testing
```

## Usage Examples

### 1. Sign an XML Document

```java
import signature.*;
import javax.xml.crypto.dsig.*;
import java.security.KeyPair;

// Generate key pair
KryptoUtil kryptoUtil = new KryptoUtil();
KeyPair keyPair = kryptoUtil.generateKeyPairs();

// Load XML document
Document doc = DocumentUtil.getXmlDocument("path/to/document.xml");

// Sign the document
Document signedDoc = XMLSignatureUtil.sign(
    doc,
    null,                                    // keyName (optional)
    keyPair,
    DigestMethod.SHA256,
    "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
    "",                                      // referenceURI
    CanonicalizationMethod.INCLUSIVE
);

// Output signed XML
System.out.println(DocumentUtil.asString(signedDoc));
```

### 2. Validate a Signed Document

```java
boolean isValid = XMLSignatureUtil.validate(signedDoc, keyPair.getPublic());
System.out.println("Signature valid: " + isValid);
```

### 3. Store and Load Keys

```java
KryptoUtil kryptoUtil = new KryptoUtil();

// Generate and store keys
kryptoUtil.storeKeyPairs("/path/to/keys/");

// Load keys later
PrivateKey privateKey = kryptoUtil.getStoredPrivateKey("/path/to/keys/privatekey.key");
PublicKey publicKey = kryptoUtil.getStoredPublicKey("/path/to/keys/publickey.key");
KeyPair keyPair = new KeyPair(publicKey, privateKey);
```

### 4. XStream XML Serialization

```java
import demo.*;
import model.*;
import com.thoughtworks.xstream.XStream;

// Create object
Company company = TestDataFactory.createCompany();

// To XML
XStream xstream = new XStream();
xstream.processAnnotations(Company.class);
xstream.processAnnotations(Address.class);
String xml = xstream.toXML(company);

// From XML
Company fromXml = (Company) xstream.fromXML(xml);
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| XStream | 1.4.20 | XML/Object serialization |
| SLF4J | 2.0.12 | Logging |
| FastJSON2 | 2.0.47 | JSON processing |
| Jettison | 1.5.4 | JSON/XML conversion |
| JUnit 5 | 5.10.2 | Testing |

## Security Notes

- RSA keys are generated with **2048-bit** key size (NIST recommended minimum)
- Uses **SHA-256** for digest and signature methods
- XStream requires explicit type permissions in production (see `AnyTypePermission`)

## License

MIT
