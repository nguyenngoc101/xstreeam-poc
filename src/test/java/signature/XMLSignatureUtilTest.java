package signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XMLSignatureUtil.
 * Tests XML digital signature creation and validation.
 */
@DisplayName("XML Signature Utility Tests")
class XMLSignatureUtilTest {

    private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    private KeyPair keyPair;
    private KryptoUtil kryptoUtil;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        kryptoUtil = new KryptoUtil();
        keyPair = kryptoUtil.generateKeyPairs();
    }

    @Test
    @DisplayName("Should sign document without X509 certificate")
    void signRootNodeWithoutX509Certificate() throws Exception {
        // Arrange
        Document doc = loadTestDocument();
        assertNotNull(doc, "Test document should be loaded");

        String signatureMethod = RSA_SHA256;
        String digestMethod = DigestMethod.SHA256;
        String referenceURI = "";
        String canonicalizationMethodType = CanonicalizationMethod.INCLUSIVE;

        // Act
        Document signedDoc = XMLSignatureUtil.sign(
                doc, null, keyPair, digestMethod, signatureMethod,
                referenceURI, canonicalizationMethodType);

        // Assert
        assertNotNull(signedDoc, "Signed document should not be null");
        assertNotNull(signedDoc.getElementsByTagNameNS(
                "http://www.w3.org/2000/09/xmldsig#", "Signature").item(0),
                "Signature element should be present");
    }

    @Test
    @DisplayName("Should sign document with X509 certificate and validate successfully")
    void signRootNodeWithKeyPairAndValidate() throws Exception {
        // Arrange
        Document doc = loadTestDocument();
        assertNotNull(doc, "Test document should be loaded");

        String signatureMethod = RSA_SHA256;
        String digestMethod = DigestMethod.SHA256;
        String referenceURI = "";
        String canonicalizationMethodType = CanonicalizationMethod.INCLUSIVE;

        // Act - Sign
        Document signedDoc = XMLSignatureUtil.sign(
                doc, null, keyPair, digestMethod, signatureMethod,
                referenceURI, canonicalizationMethodType);

        // Assert - Signature exists
        assertNotNull(signedDoc, "Signed document should not be null");

        // Act & Assert - Validate with correct key
        boolean isValid = XMLSignatureUtil.validate(signedDoc, keyPair.getPublic());
        assertTrue(isValid, "Signature should be valid with correct public key");
    }

    @Test
    @DisplayName("Should fail validation after document tampering")
    void signatureInvalidAfterTampering() throws Exception {
        // Arrange
        Document doc = loadTestDocument();
        String signatureMethod = RSA_SHA256;
        String digestMethod = DigestMethod.SHA256;
        String referenceURI = "";
        String canonicalizationMethodType = CanonicalizationMethod.INCLUSIVE;

        // Act - Sign
        Document signedDoc = XMLSignatureUtil.sign(
                doc, null, keyPair, digestMethod, signatureMethod,
                referenceURI, canonicalizationMethodType);

        // Verify signature is initially valid
        assertTrue(XMLSignatureUtil.validate(signedDoc, keyPair.getPublic()),
                "Signature should be valid before tampering");

        // Act - Tamper with document
        Element root = signedDoc.getDocumentElement();
        Element tamperedElement = signedDoc.createElement("TamperedNode");
        tamperedElement.setTextContent("Malicious content");
        root.appendChild(tamperedElement);

        // Assert - Signature should now be invalid
        boolean isValidAfterTampering = XMLSignatureUtil.validate(signedDoc, keyPair.getPublic());
        assertFalse(isValidAfterTampering, "Signature should be invalid after tampering");
    }

    @Test
    @DisplayName("Should fail validation with wrong key")
    void signatureInvalidWithWrongKey() throws Exception {
        // Arrange
        Document doc = loadTestDocument();
        KeyPair differentKeyPair = kryptoUtil.generateKeyPairs();

        String signatureMethod = RSA_SHA256;
        String digestMethod = DigestMethod.SHA256;
        String referenceURI = "";
        String canonicalizationMethodType = CanonicalizationMethod.INCLUSIVE;

        // Act - Sign with original key pair
        Document signedDoc = XMLSignatureUtil.sign(
                doc, null, keyPair, digestMethod, signatureMethod,
                referenceURI, canonicalizationMethodType);

        // Assert - Validate with different key should fail
        boolean isValid = XMLSignatureUtil.validate(signedDoc, differentKeyPair.getPublic());
        assertFalse(isValid, "Signature should be invalid with wrong public key");
    }

    @Test
    @DisplayName("Should throw exception for null document in validate")
    void validateThrowsExceptionForNullDocument() {
        assertThrows(IllegalArgumentException.class,
                () -> XMLSignatureUtil.validate(null, keyPair.getPublic()),
                "Should throw IllegalArgumentException for null document");
    }

    @Test
    @DisplayName("Should throw exception for null public key in validate")
    void validateThrowsExceptionForNullKey() throws Exception {
        Document doc = loadTestDocument();
        String signatureMethod = RSA_SHA256;
        String digestMethod = DigestMethod.SHA256;

        Document signedDoc = XMLSignatureUtil.sign(
                doc, null, keyPair, digestMethod, signatureMethod,
                "", CanonicalizationMethod.INCLUSIVE);

        assertThrows(IllegalArgumentException.class,
                () -> XMLSignatureUtil.validate(signedDoc, null),
                "Should throw IllegalArgumentException for null key");
    }

    @Test
    @DisplayName("Should return false when no signature element exists")
    void validateReturnsFalseForUnsignedDocument() throws Exception {
        Document doc = loadTestDocument();
        boolean isValid = XMLSignatureUtil.validate(doc, keyPair.getPublic());
        assertFalse(isValid, "Unsigned document should not validate");
    }

    private Document loadTestDocument() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("purchase.xml")) {
            assertNotNull(is, "purchase.xml should exist in test resources");
            return DocumentUtil.parseXmlDocument(is);
        }
    }
}
