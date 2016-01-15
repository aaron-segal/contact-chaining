package cc;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class TelecomKeys extends Keys {

	private PrivateKey privateKey;
	private Cipher decrypter;

	public TelecomKeys(String privateKeyFilename, String publicKeyFilename,
			String keysPath, int id, int[] agencyIds) throws IOException {
		super(privateKeyFilename, publicKeyFilename, keysPath, id, agencyIds);
		loadPrivateKey(privateKeyFilename);
	}

	private void loadPrivateKey(String privateKeyFilename) throws IOException {
		FileInputStream privateKeyInput = new FileInputStream(privateKeyFilename);
		byte[] privateKeyBytes = new byte[1024];
		int read = privateKeyInput.read(privateKeyBytes);
		privateKeyBytes = Arrays.copyOf(privateKeyBytes, read);
		privateKeyInput.close();
		// Parse it as our private key.
		PKCS8EncodedKeySpec privateKeySpec =
				new PKCS8EncodedKeySpec(privateKeyBytes);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(CryptoKeyGen.ENCRYPTION_ALGORITHM);
			privateKey = keyFactory.generatePrivate(privateKeySpec);
			decrypter = Cipher.getInstance(CryptoKeyGen.ENCRYPTION_ALGORITHM + PADDING);
			decrypter.init(Cipher.DECRYPT_MODE, privateKey);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Decrypts a telecom ciphertext into an integer.
	 * @param ciphertext The ciphertext to decrypt.
	 * @return The plaintext integer of this ciphertext.
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public int decrypt(byte[] ciphertext) throws IllegalBlockSizeException, BadPaddingException {
		byte[] byteData = decrypter.doFinal(ciphertext);
		return new BigInteger(byteData).intValue();
	}

}
