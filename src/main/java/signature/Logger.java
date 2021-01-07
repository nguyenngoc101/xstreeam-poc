package signature;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;

public class Logger {
    public String couldNotCreateInstance(String dom, Exception err) {
        return null;
    }

    public IllegalArgumentException nullArgumentError(String node_to_be_signed) {
        return null;
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public void trace(String s) {

    }

    public void debug(String cannot_find_signature_element) {

    }

    public RuntimeException nullValueError(String public_key) {
        return null;
    }

    public void trace(Exception ex) {

    }

    public RuntimeException processingError(CertificateException e) {
        return null;
    }
}
