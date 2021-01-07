package signature;


import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.transform.TransformerException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class XMLSignatureUtilTest {

    private final static String RSA_SH256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    @Test
    public void signRootNodeWithoutX509Certificate() throws MarshalException, GeneralSecurityException, XMLSignatureException, TransformerException {
        //Document sign(Document doc, String keyName, KeyPair keyPair, String digestMethod,
        // String signatureMethod, String referenceURI, String canonicalizationMethodType)

        KryptoUtil kryptoUtil = new KryptoUtil();
        PrivateKey privateKey = kryptoUtil.getStoredPrivateKey("/Users/ngocnv/workspace/xstream-poc/privatekey.key");
        PublicKey publicKey = kryptoUtil.getStoredPublicKey("/Users/ngocnv/workspace/xstream-poc/publickey.key");
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        Document doc = DocumentUtil.getXmlDocument("/Users/ngocnv/workspace/xstream-poc/src/test/java/signature/purchase.xml");
        String keyName = null;
        String signatureMethod = RSA_SH256;
        String digestMethod = DigestMethod.SHA256;
        String referenceURI = "";
        String canonicalizationMethodType = CanonicalizationMethod.INCLUSIVE;
        Document signedDoc = XMLSignatureUtil.sign(doc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, canonicalizationMethodType);
        System.out.println("Signed Document"+DocumentUtil.asString(signedDoc));
    }

    @Test
    public void signRootNodeWithX509Certificate() throws MarshalException, GeneralSecurityException, XMLSignatureException, TransformerException, FileNotFoundException {

        KryptoUtil kryptoUtil = new KryptoUtil();
        PrivateKey privateKey = kryptoUtil.getStoredPrivateKey("/Users/ngocnv/workspace/xstream-poc/privatekey.key");
        PublicKey publicKey = kryptoUtil.getStoredPublicKey("/Users/ngocnv/workspace/xstream-poc/publickey.key");
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        Document doc = DocumentUtil.getXmlDocument("/Users/ngocnv/workspace/xstream-poc/src/test/java/signature/purchase.xml");
        String keyName = null;
        String signatureMethod = RSA_SH256;
        String digestMethod = DigestMethod.SHA256;
        String referenceURI = "";
        String canonicalizationMethodType = CanonicalizationMethod.INCLUSIVE;

        //keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
        //keytool -exportcert -alias selfsigned -keystore keystore.jks -file instapay.cert
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        FileInputStream is = new FileInputStream ("/Users/ngocnv/workspace/xstream-poc/instapay.cert");
        X509Certificate x509Certificate = (X509Certificate) fact.generateCertificate(is);

        Document signedDoc = XMLSignatureUtil.sign(doc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, x509Certificate, canonicalizationMethodType);
        System.out.println("Signed Document"+DocumentUtil.asString(signedDoc));
    }

}