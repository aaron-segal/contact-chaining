package cc;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class TelecomKeys extends Keys {

	private PrivateKey privateKey;
	private Cipher[] decrypters;
	private HashMap<Integer, Cipher[]> multiEncrypters;

	public TelecomKeys(String privateKeyFilename, String publicKeyFilename,
			String keysPath, int id, int[] agencyIds, int numThreads) throws IOException {
		super(privateKeyFilename, publicKeyFilename, keysPath, id, agencyIds);
		loadPrivateKey(privateKeyFilename, numThreads);
		duplicateEncrypters(numThreads);
	}

	private void loadPrivateKey(String privateKeyFilename, int numThreads) throws IOException {
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
			decrypters = new Cipher[numThreads];
			for (int i = 0; i < decrypters.length; i++) {
				decrypters[i] = Cipher.getInstance(CryptoKeyGen.ENCRYPTION_ALGORITHM + PADDING);
				decrypters[i].init(Cipher.DECRYPT_MODE, privateKey);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	private void duplicateEncrypters(int copies) {
		multiEncrypters = new HashMap<Integer, Cipher[]>();
		for (int telecomId : encrypters.keySet()) {
			multiEncrypters.put(telecomId, new Cipher[copies]);
			multiEncrypters.get(telecomId)[0] = encrypters.get(telecomId);
			for (int i = 1; i < copies; i++) {
				try {
					multiEncrypters.get(telecomId)[i] = Cipher.getInstance(CryptoKeyGen.ENCRYPTION_ALGORITHM + PADDING);
					multiEncrypters.get(telecomId)[i].init(Cipher.ENCRYPT_MODE, telecomPublicKeys.get(telecomId));
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
					return;
				}
			}
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
		byte[] byteData = decrypters[0].doFinal(ciphertext);
		return new BigInteger(byteData).intValue();
	}
	
	/**
	 * Decrypts a telecom ciphertext into an integer using a specific decrypter.
	 * @param ciphertext The ciphertext to decrypt.
	 * @param threadId The thread doing the encrypting.
	 * @return The plaintext integer of this ciphertext.
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public int decrypt(byte[] ciphertext, int threadId) throws IllegalBlockSizeException, BadPaddingException {
		byte[] byteData = decrypters[threadId].doFinal(ciphertext);
		return new BigInteger(byteData).intValue();
	}

	/**
	 * Encrypts an integer into a telecom ciphertext using a specific encrypter.
	 * Used for multi-thread encryption.
	 * @param receiverId The telecom who will receive this ciphertext.
	 * @param data The integer to encrypt
	 * @param threadId The thread doing the encrypting.
	 * @return The encrypted data.
	 */
	public byte[] encrypt(int receiverId, int data, int threadId) {
		byte[] byteData = BigInteger.valueOf(data).toByteArray();
		try {
			return multiEncrypters.get(receiverId)[threadId].doFinal(byteData);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

}
