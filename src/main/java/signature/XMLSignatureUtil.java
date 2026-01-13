package signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for XML Digital Signature operations.
 * Provides methods for signing and validating XML documents using the XML-DSig standard.
 */
public class XMLSignatureUtil {

    private static final Logger logger = LoggerFactory.getLogger(XMLSignatureUtil.class);
    private static final XMLSignatureFactory signatureFactory = getXMLSignatureFactory();

    private static boolean includeKeyInfoInSignature = true;

    private XMLSignatureUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * KeySelector that uses a preset key for signature validation.
     */
    private static class KeySelectorPresetKey extends KeySelector {
        private final Key key;

        public KeySelectorPresetKey(Key key) {
            this.key = key;
        }

        @Override
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) {
            return () -> key;
        }
    }

    private static XMLSignatureFactory getXMLSignatureFactory() {
        try {
            return XMLSignatureFactory.getInstance("DOM", "ApacheXMLDSig");
        } catch (NoSuchProviderException ex) {
            logger.debug("ApacheXMLDSig provider not found, using default DOM provider");
            try {
                return XMLSignatureFactory.getInstance("DOM");
            } catch (Exception err) {
                throw new IllegalStateException("Could not create XMLSignatureFactory for DOM", err);
            }
        }
    }

    /**
     * Signs a specific node in a document.
     *
     * @param doc the document containing the node
     * @param nodeToBeSigned the node to sign
     * @param keyName optional key name for KeyInfo
     * @param keyPair the key pair for signing
     * @param digestMethod the digest method URI
     * @param signatureMethod the signature method URI
     * @param referenceURI the reference URI
     * @param x509Certificate optional X509 certificate to include
     * @param canonicalizationMethodType the canonicalization method
     * @return the signed document
     */
    public static Document sign(Document doc, Node nodeToBeSigned, String keyName, KeyPair keyPair,
                                String digestMethod, String signatureMethod, String referenceURI,
                                X509Certificate x509Certificate, String canonicalizationMethodType)
            throws ParserConfigurationException, GeneralSecurityException, MarshalException,
            XMLSignatureException, TransformerException {

        if (nodeToBeSigned == null) {
            throw new IllegalArgumentException("Node to be signed cannot be null");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Document to be signed={}", DocumentUtil.asString(doc));
        }

        Node parentNode = nodeToBeSigned.getParentNode();

        // Create a new Document for signing
        Document newDoc = DocumentUtil.createDocument();
        Node signingNode = newDoc.importNode(nodeToBeSigned, true);
        newDoc.appendChild(signingNode);

        if (!referenceURI.isEmpty()) {
            propagateIDAttributeSetup(nodeToBeSigned, newDoc.getDocumentElement());
        }

        newDoc = sign(newDoc, keyName, keyPair, digestMethod, signatureMethod, referenceURI,
                x509Certificate, canonicalizationMethodType);

        // Import signed node back into original document
        Node signedNode = doc.importNode(newDoc.getFirstChild(), true);

        if (!referenceURI.isEmpty()) {
            propagateIDAttributeSetup(newDoc.getDocumentElement(), (Element) signedNode);
        }

        parentNode.replaceChild(signedNode, nodeToBeSigned);

        return doc;
    }

    /**
     * Signs a specific element with the signature placed before a sibling node.
     *
     * @param elementToSign element to sign with set ID
     * @param nextSibling child of elementToSign, used as next sibling of created signature
     * @param keyName optional key name
     * @param keyPair the key pair for signing
     * @param digestMethod the digest method URI
     * @param signatureMethod the signature method URI
     * @param referenceURI the reference URI
     * @param canonicalizationMethodType the canonicalization method
     */
    public static void sign(Element elementToSign, Node nextSibling, String keyName, KeyPair keyPair,
                            String digestMethod, String signatureMethod, String referenceURI,
                            String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        sign(elementToSign, nextSibling, keyName, keyPair, digestMethod, signatureMethod,
                referenceURI, null, canonicalizationMethodType);
    }

    /**
     * Signs a specific element with X509 certificate included.
     *
     * @param elementToSign element to sign with set ID
     * @param nextSibling child of elementToSign, used as next sibling of created signature
     * @param keyName optional key name
     * @param keyPair the key pair for signing
     * @param digestMethod the digest method URI
     * @param signatureMethod the signature method URI
     * @param referenceURI the reference URI
     * @param x509Certificate X509 certificate to place in SignedInfo
     * @param canonicalizationMethodType the canonicalization method
     */
    public static void sign(Element elementToSign, Node nextSibling, String keyName, KeyPair keyPair,
                            String digestMethod, String signatureMethod, String referenceURI,
                            X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {

        PrivateKey signingKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        DOMSignContext signContext = new DOMSignContext(signingKey, elementToSign, nextSibling);
        signImpl(signContext, digestMethod, signatureMethod, referenceURI, keyName, publicKey,
                x509Certificate, canonicalizationMethodType);
    }

    /**
     * Signs the root element of a document.
     *
     * @param doc the document to sign
     * @param keyName optional key name
     * @param keyPair the key pair for signing
     * @param digestMethod the digest method URI
     * @param signatureMethod the signature method URI
     * @param referenceURI the reference URI
     * @param canonicalizationMethodType the canonicalization method
     * @return the signed document
     */
    public static Document sign(Document doc, String keyName, KeyPair keyPair, String digestMethod,
                                String signatureMethod, String referenceURI,
                                String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException, TransformerException {
        return sign(doc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, null,
                canonicalizationMethodType);
    }

    /**
     * Signs the root element of a document with X509 certificate.
     *
     * @param doc the document to sign
     * @param keyName optional key name
     * @param keyPair the key pair for signing
     * @param digestMethod the digest method URI
     * @param signatureMethod the signature method URI
     * @param referenceURI the reference URI
     * @param x509Certificate X509 certificate to include
     * @param canonicalizationMethodType the canonicalization method
     * @return the signed document
     */
    public static Document sign(Document doc, String keyName, KeyPair keyPair, String digestMethod,
                                String signatureMethod, String referenceURI,
                                X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException, TransformerException {

        if (logger.isTraceEnabled()) {
            logger.trace("Document to be signed={}", DocumentUtil.asString(doc));
        }

        PrivateKey signingKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        DOMSignContext signContext = new DOMSignContext(signingKey, doc.getDocumentElement());
        signImpl(signContext, digestMethod, signatureMethod, referenceURI, keyName, publicKey,
                x509Certificate, canonicalizationMethodType);

        return doc;
    }

    /**
     * Validates all signatures in a signed document.
     *
     * @param signedDoc the signed document
     * @param publicKey the public key for validation
     * @return true if all signatures are valid
     */
    public static boolean validate(Document signedDoc, Key publicKey)
            throws MarshalException, XMLSignatureException {

        if (signedDoc == null) {
            throw new IllegalArgumentException("Signed document cannot be null");
        }

        propagateIDAttributeSetup(signedDoc.getDocumentElement(), signedDoc.getDocumentElement());

        NodeList signatureNodes = signedDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

        if (signatureNodes == null || signatureNodes.getLength() == 0) {
            logger.debug("Cannot find Signature element");
            return false;
        }

        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }

        for (int i = 0; i < signatureNodes.getLength(); i++) {
            Node signatureNode = signatureNodes.item(i);
            if (!validateSingleNode(signatureNode, publicKey)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates a single signature node.
     *
     * @param signatureNode the signature node to validate
     * @param key the key for validation
     * @return true if signature is valid
     */
    public static boolean validateSingleNode(Node signatureNode, Key key)
            throws MarshalException, XMLSignatureException {
        try {
            if (validateUsingKeySelector(signatureNode, new KeySelectorPresetKey(key))) {
                return true;
            }
        } catch (XMLSignatureException ex) {
            logger.debug("Verification failed: {}", ex.getMessage());
            logger.trace("Verification exception", ex);
        }
        return false;
    }

    private static boolean validateUsingKeySelector(Node signatureNode, KeySelector keySelector)
            throws XMLSignatureException, MarshalException {

        DOMValidateContext validateContext = new DOMValidateContext(keySelector, signatureNode);
        XMLSignature signature = signatureFactory.unmarshalXMLSignature(validateContext);
        boolean coreValidity = signature.validate(validateContext);

        if (!coreValidity && logger.isTraceEnabled()) {
            boolean signatureValid = signature.getSignatureValue().validate(validateContext);
            logger.trace("Signature validation status: {}", signatureValid);

            List<Reference> references = signature.getSignedInfo().getReferences();
            for (Reference ref : references) {
                logger.trace("[Ref id={}:uri={}] validity status: {}",
                        ref.getId(), ref.getURI(), ref.validate(validateContext));
            }
        }

        return coreValidity;
    }

    /**
     * Parses an X509 certificate from a KeyInfo certificate string.
     *
     * @param certificateString the base64-encoded certificate string
     * @return the parsed X509Certificate
     * @throws CertificateException if certificate parsing fails
     */
    public static X509Certificate getX509CertificateFromKeyInfoString(String certificateString)
            throws CertificateException {

        String pemCertificate = "-----BEGIN CERTIFICATE-----\n" +
                certificateString +
                "\n-----END CERTIFICATE-----";

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                pemCertificate.getBytes(StandardCharsets.UTF_8));

        return (X509Certificate) certFactory.generateCertificate(inputStream);
    }

    private static void signImpl(DOMSignContext signContext, String digestMethod,
                                 String signatureMethod, String referenceURI, String keyName,
                                 PublicKey publicKey, X509Certificate x509Certificate,
                                 String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {

        DigestMethod digestMethodObj = signatureFactory.newDigestMethod(digestMethod, null);

        List<Transform> transforms = new ArrayList<>();
        transforms.add(signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(signatureFactory.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#",
                (TransformParameterSpec) null));

        Reference reference = signatureFactory.newReference(referenceURI, digestMethodObj,
                transforms, null, null);

        CanonicalizationMethod canonicalizationMethod = signatureFactory.newCanonicalizationMethod(
                canonicalizationMethodType, (C14NMethodParameterSpec) null);

        List<Reference> references = Collections.singletonList(reference);
        SignatureMethod signatureMethodObj = signatureFactory.newSignatureMethod(signatureMethod, null);
        SignedInfo signedInfo = signatureFactory.newSignedInfo(canonicalizationMethod,
                signatureMethodObj, references);

        KeyInfo keyInfo;
        if (includeKeyInfoInSignature) {
            keyInfo = createKeyInfo(keyName, publicKey, x509Certificate);
        } else {
            keyInfo = createKeyInfo(keyName, null, null);
        }

        XMLSignature signature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
        signature.sign(signContext);
    }

    private static KeyInfo createKeyInfo(String keyName, PublicKey publicKey,
                                         X509Certificate x509Certificate) throws KeyException {

        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        List<XMLStructure> items = new LinkedList<>();

        if (keyName != null) {
            items.add(keyInfoFactory.newKeyName(keyName));
        }

        if (x509Certificate != null) {
            items.add(keyInfoFactory.newX509Data(Collections.singletonList(x509Certificate)));
        }

        if (publicKey != null) {
            items.add(keyInfoFactory.newKeyValue(publicKey));
        }

        return keyInfoFactory.newKeyInfo(items);
    }

    /**
     * Propagates ID attribute setup from source node to destination element.
     *
     * @param sourceNode the source node with ID attribute
     * @param destElement the destination element
     */
    public static void propagateIDAttributeSetup(Node sourceNode, Element destElement) {
        NamedNodeMap attributes = sourceNode.getAttributes();
        if (attributes == null) {
            return;
        }

        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            if (attr.isId()) {
                destElement.setIdAttribute(attr.getName(), true);
                break;
            }
        }
    }

    /**
     * Sets whether to include KeyInfo in signatures.
     *
     * @param include true to include KeyInfo
     */
    public static void setIncludeKeyInfoInSignature(boolean include) {
        includeKeyInfoInSignature = include;
    }
}
