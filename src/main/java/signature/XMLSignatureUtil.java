package signature;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.MarshalException;
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
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.transform.TransformerException;

public class XMLSignatureUtil {

    private static final Logger logger = new Logger();
    private static final XMLSignatureFactory fac = getXMLSignatureFactory();

    /**
     * By default, we include the keyinfo in the signature
     */
    private static boolean includeKeyInfoInSignature = true;

    private static class KeySelectorPresetKey extends KeySelector {

        private final Key key;

        public KeySelectorPresetKey(Key key) {
            this.key = key;
        }

        @Override
        public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) {
            return new KeySelectorResult() {
                @Override public Key getKey() {
                    return key;
                }
            };
        }
    }

    private static XMLSignatureFactory getXMLSignatureFactory() {
        XMLSignatureFactory xsf = null;

        try {
            xsf = XMLSignatureFactory.getInstance("DOM", "ApacheXMLDSig");
        } catch (NoSuchProviderException ex) {
            try {
                xsf = XMLSignatureFactory.getInstance("DOM");
            } catch (Exception err) {
                throw new RuntimeException(logger.couldNotCreateInstance("DOM", err));
            }
        }
        return xsf;
    }

    /**
     * Sign a node in a document
     *
     * @param doc
     * @param nodeToBeSigned
     * @param keyPair
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @throws GeneralSecurityException
     */
    public static Document sign(Document doc, Node nodeToBeSigned, String keyName, KeyPair keyPair, String digestMethod,
                                String signatureMethod, String referenceURI, X509Certificate x509Certificate,
                                String canonicalizationMethodType) throws ParserConfigurationException, GeneralSecurityException,
            MarshalException, XMLSignatureException, TransformerException {
        if (nodeToBeSigned == null)
            throw logger.nullArgumentError("Node to be signed");

        if (logger.isTraceEnabled()) {
            logger.trace("Document to be signed=" + DocumentUtil.asString(doc));
        }

        Node parentNode = nodeToBeSigned.getParentNode();

        // Let us create a new Document
        Document newDoc = DocumentUtil.createDocument();
        // Import the node
        Node signingNode = newDoc.importNode(nodeToBeSigned, true);
        newDoc.appendChild(signingNode);

        if (!referenceURI.isEmpty()) {
            propagateIDAttributeSetup(nodeToBeSigned, newDoc.getDocumentElement());
        }
        newDoc = sign(newDoc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, x509Certificate, canonicalizationMethodType);

        // Now let us import this signed doc into the original document we got in the method call
        Node signedNode = doc.importNode(newDoc.getFirstChild(), true);

        if (!referenceURI.isEmpty()) {
            propagateIDAttributeSetup(newDoc.getDocumentElement(), (Element) signedNode);
        }

        parentNode.replaceChild(signedNode, nodeToBeSigned);
        // doc.getDocumentElement().replaceChild(signedNode, nodeToBeSigned);

        return doc;
    }

    /**
     * Sign only specified element (assumption is that it already has ID attribute set)
     *
     * @param elementToSign element to sign with set ID
     * @param nextSibling child of elementToSign, which will be used as next sibling of created signature
     * @param keyPair
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @throws GeneralSecurityException
     * @throws MarshalException
     * @throws XMLSignatureException
     */
    public static void sign(Element elementToSign, Node nextSibling, String keyName, KeyPair keyPair, String digestMethod,
                            String signatureMethod, String referenceURI, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        sign(elementToSign, nextSibling, keyName, keyPair, digestMethod, signatureMethod, referenceURI, null, canonicalizationMethodType);
    }

    /**
     * Sign only specified element (assumption is that it already has ID attribute set)
     *
     * @param elementToSign element to sign with set ID
     * @param nextSibling child of elementToSign, which will be used as next sibling of created signature
     * @param keyPair
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     * @param x509Certificate {@link X509Certificate} to be placed in SignedInfo
     *
     * @throws GeneralSecurityException
     * @throws MarshalException
     * @throws XMLSignatureException
     * @since 2.5.0
     */
    public static void sign(Element elementToSign, Node nextSibling, String keyName, KeyPair keyPair, String digestMethod,
                            String signatureMethod, String referenceURI, X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        PrivateKey signingKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        DOMSignContext dsc = new DOMSignContext(signingKey, elementToSign, nextSibling);

        signImpl(dsc, digestMethod, signatureMethod, referenceURI, keyName, publicKey, x509Certificate, canonicalizationMethodType);
    }

    /**
     * Sign the root element
     *
     * @param doc
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws XMLSignatureException
     * @throws MarshalException
     */
    public static Document sign(Document doc, String keyName, KeyPair keyPair, String digestMethod, String signatureMethod, String referenceURI, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException, TransformerException {
        return sign(doc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, null, canonicalizationMethodType);
    }

    /**
     * Sign the root element
     *
     * @param doc
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @since 2.5.0
     */
    public static Document sign(Document doc, String keyName, KeyPair keyPair, String digestMethod, String signatureMethod, String referenceURI,
                                X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException, TransformerException {
        if (logger.isTraceEnabled()) {
            logger.trace("Document to be signed=" + DocumentUtil.asString(doc));
        }
        PrivateKey signingKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        DOMSignContext dsc = new DOMSignContext(signingKey, doc.getDocumentElement());

        signImpl(dsc, digestMethod, signatureMethod, referenceURI, keyName, publicKey, x509Certificate, canonicalizationMethodType);

        return doc;
    }

    /**
     * Validate a signed document with the given public key. All elements that contain a Signature are checked,
     * this way both assertions and the containing document are verified when signed.
     *
     * @param signedDoc
     * @param publicKey
     *
     * @return
     *
     * @throws MarshalException
     * @throws XMLSignatureException
     */
    @SuppressWarnings("unchecked")
    public static boolean validate(Document signedDoc, final Key publicKey) throws MarshalException, XMLSignatureException {
        if (signedDoc == null)
            throw logger.nullArgumentError("Signed Document");

        propagateIDAttributeSetup(signedDoc.getDocumentElement(), signedDoc.getDocumentElement());

        NodeList nl = signedDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

        if (nl == null || nl.getLength() == 0) {
            logger.debug("Cannot find Signature element");
            return false;
        }

        if (publicKey == null)
            throw logger.nullValueError("Public Key");

        for (int i = 0; i < nl.getLength(); i++) {
            Node signatureNode = nl.item(i);

            if (! validateSingleNode(signatureNode, publicKey)) return false;
        }

        return true;
    }

    public static boolean validateSingleNode(Node signatureNode, final Key key) throws MarshalException, XMLSignatureException {
        try {
            if (validateUsingKeySelector(signatureNode, new KeySelectorPresetKey(key))) {
                return true;
            }
        } catch (XMLSignatureException ex) { // pass through MarshalException
            logger.debug("Verification failed: " + ex);
            logger.trace(ex);
        }

        return false;
    }

    private static boolean validateUsingKeySelector(Node signatureNode, KeySelector validationKeySelector) throws XMLSignatureException, MarshalException {
        DOMValidateContext valContext = new DOMValidateContext(validationKeySelector, signatureNode);
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        boolean coreValidity = signature.validate(valContext);

        if (! coreValidity) {
            if (logger.isTraceEnabled()) {
                boolean sv = signature.getSignatureValue().validate(valContext);
                logger.trace("Signature validation status: " + sv);

                List<Reference> references = signature.getSignedInfo().getReferences();
                for (Reference ref : references) {
                    logger.trace("[Ref id=" + ref.getId() + ":uri=" + ref.getURI() + "]validity status:" + ref.validate(valContext));
                }
            }
        }

        return coreValidity;
    }

    /**
     * Given the X509Certificate in the keyinfo element, get a {@link X509Certificate}
     *
     * @param certificateString
     *
     * @return
     *
     */
    public static X509Certificate getX509CertificateFromKeyInfoString(String certificateString) {
        X509Certificate cert = null;
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN CERTIFICATE-----\n").append(certificateString).append("\n-----END CERTIFICATE-----");

        String derFormattedString = builder.toString();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(derFormattedString.getBytes());

            while (bais.available() > 0) {
                cert = (X509Certificate) cf.generateCertificate(bais);
            }
        } catch (java.security.cert.CertificateException e) {
            throw logger.processingError(e);
        }
        return cert;
    }

    private static void signImpl(DOMSignContext dsc, String digestMethod, String signatureMethod, String referenceURI, String keyName, PublicKey publicKey,
                                 X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        dsc.setDefaultNamespacePrefix("dsig");

        DigestMethod digestMethodObj = fac.newDigestMethod(digestMethod, null);
        Transform transform1 = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Transform transform2 = fac.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec) null);

        List<Transform> transformList = new ArrayList();
        transformList.add(transform1);
        transformList.add(transform2);

        Reference ref = fac.newReference(referenceURI, digestMethodObj, transformList, null, null);

        CanonicalizationMethod canonicalizationMethod = fac.newCanonicalizationMethod(canonicalizationMethodType,
                (C14NMethodParameterSpec) null);

        List<Reference> referenceList = Collections.singletonList(ref);
        SignatureMethod signatureMethodObj = fac.newSignatureMethod(signatureMethod, null);
        SignedInfo si = fac.newSignedInfo(canonicalizationMethod, signatureMethodObj, referenceList);

        KeyInfo ki;
        if (includeKeyInfoInSignature) {
            ki = createKeyInfo(keyName, publicKey, x509Certificate);
        } else {
            ki = createKeyInfo(keyName, null, null);
        }
        XMLSignature signature = fac.newXMLSignature(si, ki);

        signature.sign(dsc);
    }

    private static KeyInfo createKeyInfo(String keyName, PublicKey publicKey, X509Certificate x509Certificate) throws KeyException {
        KeyInfoFactory keyInfoFactory = fac.getKeyInfoFactory();

        List<XMLStructure> items = new LinkedList();

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
     * Setup the ID attribute into <code>destElement</code> depending on the <code>isId</code> flag of an attribute of
     * <code>sourceNode</code>.
     *
     * @param sourceNode
     */
    public static void propagateIDAttributeSetup(Node sourceNode, Element destElement) {
        NamedNodeMap nnm = sourceNode.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            Attr attr = (Attr) nnm.item(i);
            if (attr.isId()) {
                destElement.setIdAttribute(attr.getName(), true);
                break;
            }
        }
    }
}