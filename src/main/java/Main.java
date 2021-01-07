import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.w3c.dom.Node;
import sun.security.rsa.RSAPublicKeyImpl;
import sun.security.x509.X509Key;

import javax.xml.crypto.*;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;

public class Main {
    public static void main(String[] args) throws MarshalException, XMLSignatureException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException {
//        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
//        DigestMethod digestMethod = fac.newDigestMethod ("http://www.w3.org/2000/09/xmldsig#sha1", null);
//        Reference ref = fac.newReference("#10",digestMethod);
//        ArrayList refList = new ArrayList();
//        refList.add(ref);
//        CanonicalizationMethod cm =  fac.newCanonicalizationMethod("http://www.w3.org/2006/12/xml-c14n11", (XMLStructure) null);
//        SignatureMethod sm = fac.newSignatureMethod("http://www.w3.org/2000/09/xmldsig#rsa-sha1",null);
//        SignedInfo signedInfo =fac.newSignedInfo(cm,sm,refList);
//        DOMSignContext signContext = null;
//        RSAPublicKey key = new RSAPublicKeyImpl(null);
//        KeySelector privKey = KeySelector.singletonKeySelector(key);
//        Node securityHeader = null;
//        signContext = new DOMSignContext(privKey,securityHeader);
//        signContext.setURIDereferencer(new URIResolverImpl());
//        KeyInfoFactory keyFactory = KeyInfoFactory.getInstance();
//        Node tokenReference = null;
//        DOMStructure domKeyInfo = new DOMStructure(tokenReference);
//        KeyInfo keyInfo = keyFactory.newKeyInfo(Collections.singletonList(domKeyInfo));
//        XMLSignature signature = fac.newXMLSignature(signedInfo,keyInfo);
//        signature.sign(signContext);


        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        DigestMethod digestMethod =
                fac.newDigestMethod("http://www.w3.org/2000/09/xmldsig#sha1", null);
        C14NMethodParameterSpec spec = null;
        CanonicalizationMethod cm = fac.newCanonicalizationMethod(
                "http://www.w3.org/2006/12/xml-c14n11",spec);
        SignatureMethod sm = fac.newSignatureMethod(
                "http://www.w3.org/2000/09/xmldsig#rsa-sha1",null);
        ArrayList transformList = new ArrayList();
        TransformParameterSpec transformSpec = null;
        Transform envTransform = fac.newTransform("http://www.w3.org/2006/12/xml-c14n11",transformSpec);
                Transform exc14nTransform = fac.newTransform(
                        "http://www.w3.org/2000/09/xmldsig#enveloped-signature",transformSpec);
        transformList.add(envTransform);
        transformList.add(exc14nTransform);
        Reference ref = fac.newReference("",digestMethod,transformList,null,null);
        ArrayList refList = new ArrayList();
        refList.add(ref);
        SignedInfo signedInfo = fac.newSignedInfo(cm,sm,refList);
        XMLSignature xmlSignature = fac.unmarshalXMLSignature((XMLStructure) null);
        xmlSignature.sign(null);
        System.out.println(signedInfo);
    }
}
