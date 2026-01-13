package signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utility class for cryptographic key operations.
 * Provides methods for generating, storing, and retrieving RSA key pairs.
 */
public class KryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(KryptoUtil.class);

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * Generates a new RSA key pair.
     *
     * @return the generated KeyPair
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     */
    public KeyPair generateKeyPairs() throws NoSuchAlgorithmException {
        logger.debug("Generating {} key pair with {} bit key size", ALGORITHM, KEY_SIZE);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    /**
     * Generates and stores a key pair to the specified directory.
     *
     * @param dirPath directory path to store the keys
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     * @throws IOException if key files cannot be written
     */
    public void storeKeyPairs(String dirPath) throws NoSuchAlgorithmException, IOException {
        KeyPair keyPair = generateKeyPairs();
        Path publicKeyPath = Path.of(dirPath, "publickey.key");
        Path privateKeyPath = Path.of(dirPath, "privatekey.key");

        storeKey(publicKeyPath, keyPair.getPublic());
        storeKey(privateKeyPath, keyPair.getPrivate());

        logger.info("Key pair stored successfully in {}", dirPath);
    }

    /**
     * Stores a key to a file.
     *
     * @param filePath path to the key file
     * @param key the key to store (public or private)
     * @throws IOException if the file cannot be written
     */
    public void storeKey(Path filePath, java.security.Key key) throws IOException {
        Files.write(filePath, key.getEncoded());
        logger.debug("Key stored to {}", filePath);
    }

    /**
     * Retrieves a stored private key from file.
     *
     * @param filePath path to the private key file
     * @return the PrivateKey
     * @throws IOException if the file cannot be read
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     * @throws InvalidKeySpecException if the key specification is invalid
     */
    public PrivateKey getStoredPrivateKey(String filePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyData = Files.readAllBytes(Path.of(filePath));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        logger.debug("Private key loaded from {}", filePath);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Retrieves a stored public key from file.
     *
     * @param filePath path to the public key file
     * @return the PublicKey
     * @throws IOException if the file cannot be read
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     * @throws InvalidKeySpecException if the key specification is invalid
     */
    public PublicKey getStoredPublicKey(String filePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyData = Files.readAllBytes(Path.of(filePath));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyData);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        logger.debug("Public key loaded from {}", filePath);
        return keyFactory.generatePublic(keySpec);
    }
}
