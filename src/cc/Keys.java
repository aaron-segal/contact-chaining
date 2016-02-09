package cc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * Stores one user's key data: a single private key, and many public keys.
 * @author Aaron Segal
 *
 */
public abstract class Keys {

	public final static String PADDING = "/ECB/PKCS1Padding"; 

	private HashMap<Integer, BigInteger> agencyPublicKeys;
	protected HashMap<Integer, PublicKey> telecomPublicKeys;
	private PrivateKey signingKey;
	private Signature signer;
	private HashMap<Integer, PublicKey> verifyKeys;
	private HashMap<Integer, Signature> verifiers;
	protected HashMap<Integer, Cipher> encrypters;
	private int[] agencyIds;
	private int id;

	public Keys(String privateKeyFilename, String publicKeyFilename,
			String keysPath, int id, int[] agencyIds)
					throws IOException {
		this.id = id;
		this.agencyIds = agencyIds;
		loadAgencyPublicKeys(publicKeyFilename);
		loadKeys(keysPath);
	}

	private void loadAgencyPublicKeys(String publicKeyFilename) throws IOException {
		agencyPublicKeys = new HashMap<Integer, BigInteger>();
		File pub = null;
		Scanner spub;
		pub = new File(publicKeyFilename);
		if (!pub.exists()) {
			throw new FileNotFoundException("Could not read from " + publicKeyFilename);
		}
		spub = new Scanner(pub.getAbsoluteFile());
		int currId = Integer.MIN_VALUE;
		while (spub.hasNextLine()) {
			String line = spub.nextLine();
			String [] lineParts = new String[2];
			if (line.startsWith("#") || line.startsWith("!")) {
				continue;
			} else if (line.contains("=")) {
				lineParts = line.split("=", 2);
				lineParts[0] = lineParts[0].trim();
				lineParts[1] = lineParts[1].trim();
			} else if (line.contains(":")) {
				lineParts = line.split(":", 2);
			} else if (line.contains(" ")) {
				lineParts = line.split(" ", 2);
			} else {
				spub.close();
				throw new IOException("Could not parse" + publicKeyFilename);
			}
			if (lineParts[0].equalsIgnoreCase(KeyGen.ID)) {
				currId = Integer.parseInt(lineParts[1]);
			} else if (lineParts[0].equalsIgnoreCase(KeyGen.PUBLIC_KEY)) {
				// We will only record this key if it belongs to an agency.
				// Other ElGamal keys are ignored.
				for (int agencyId : agencyIds) {
					if (currId == agencyId) {
						agencyPublicKeys.put(currId, new BigInteger(lineParts[1]));
						break;
					}
				}
				currId = Integer.MIN_VALUE;
			}
		}
		spub.close(); 
	}

	private void loadKeys(String signatureKeysPath) throws IOException {
		verifyKeys = new HashMap<Integer,PublicKey>();
		verifiers = new HashMap<Integer, Signature>();
		telecomPublicKeys = new HashMap<Integer, PublicKey>();
		encrypters = new HashMap<Integer, Cipher>();
		File path = new File(signatureKeysPath);
		for (File keyFile : path.listFiles()) {
			if (keyFile.getName().equals(CryptoKeyGen.SIGNING_PREFIX + id +
					CryptoKeyGen.SUFFIX)) {
				// If this is our signing key, read it
				FileInputStream signingKeyInput = new FileInputStream(keyFile);
				byte[] signingKeyBytes = new byte[2048];
				int read = signingKeyInput.read(signingKeyBytes);
				signingKeyBytes = Arrays.copyOf(signingKeyBytes, read);
				signingKeyInput.close();
				// and parse it as our signing key.
				PKCS8EncodedKeySpec signKeySpec =
						new PKCS8EncodedKeySpec(signingKeyBytes);
				try {
					KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
					signingKey = keyFactory.generatePrivate(signKeySpec);
					signer = Signature.getInstance("SHA1withDSA", "SUN");
					signer.initSign(signingKey);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			} else if (keyFile.getName().startsWith(CryptoKeyGen.VERIFY_PREFIX)) {
				// If this is a verification key, get its id
				String strippedFilename = keyFile.getName();
				strippedFilename = strippedFilename.replaceAll(CryptoKeyGen.VERIFY_PREFIX,"");
				strippedFilename = strippedFilename.replaceAll(CryptoKeyGen.SUFFIX,"");
				int keyId = Integer.parseInt(strippedFilename);
				// read it
				FileInputStream verificationKeyInput = new FileInputStream(keyFile);
				byte[] verificationKeyBytes = new byte[2048];
				int read = verificationKeyInput.read(verificationKeyBytes);
				verificationKeyBytes = Arrays.copyOf(verificationKeyBytes, read);
				verificationKeyInput.close();
				// and parse it as a verification key.
				X509EncodedKeySpec verKeySpec =
						new X509EncodedKeySpec(verificationKeyBytes);
				try {
					KeyFactory keyFactory = KeyFactory.getInstance(CryptoKeyGen.SIGNING_ALGORITHM, "SUN");
					verifyKeys.put(keyId, keyFactory.generatePublic(verKeySpec));
					Signature verifier = Signature.getInstance("SHA1withDSA", "SUN");
					verifier.initVerify(verifyKeys.get(keyId));
					verifiers.put(keyId, verifier);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			} else if (keyFile.getName().startsWith(CryptoKeyGen.PUBLIC_PREFIX)) {
				// If this is a public key, get its id
				String strippedFilename = keyFile.getName();
				strippedFilename = strippedFilename.replaceAll(CryptoKeyGen.PUBLIC_PREFIX,"");
				strippedFilename = strippedFilename.replaceAll(CryptoKeyGen.SUFFIX,"");
				int keyId = Integer.parseInt(strippedFilename);
				// read it
				FileInputStream publicKeyInput = new FileInputStream(keyFile);
				byte[] publicKeyBytes = new byte[2048];
				int read = publicKeyInput.read(publicKeyBytes);
				publicKeyBytes = Arrays.copyOf(publicKeyBytes, read);
				publicKeyInput.close();
				// and parse it as a public key.
				X509EncodedKeySpec pubKeySpec =
						new X509EncodedKeySpec(publicKeyBytes);
				try {
					KeyFactory keyFactory = KeyFactory.getInstance(CryptoKeyGen.ENCRYPTION_ALGORITHM);
					telecomPublicKeys.put(keyId, keyFactory.generatePublic(pubKeySpec));
					Cipher encrypter = Cipher.getInstance(CryptoKeyGen.ENCRYPTION_ALGORITHM + PADDING);
					encrypter.init(Cipher.ENCRYPT_MODE, telecomPublicKeys.get(keyId));
					encrypters.put(keyId, encrypter);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	public BigInteger getAgencyPublicKey(int keyId) {
		return agencyPublicKeys.get(keyId);
	}

	/**
	 * @return the agencyIds
	 */
	public int[] getAgencyIds() {
		return agencyIds;
	}

	/**
	 * @param agencyIds the agencyIds to set
	 */
	public void setAgencyIds(int[] agencyIds) {
		this.agencyIds = agencyIds;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return The Signature object for signing.
	 */
	public Signature getSigner() {
		return signer;
	}

	/**
	 * Returns a Signature object for verifying another user's signature
	 * @param id The user ID whose signature you want to verify
	 * @return The Signature object for verification.
	 */
	public Signature getVerifier(int id) {
		return verifiers.get(id);
	}

	/**
	 * Uses our keys to sign some telecom ciphertexts.
	 * @param ciphertext The telecom ciphertexts to sign.
	 * @return This party's signature on these ciphertexts.
	 */
	public byte[] sign(TelecomCiphertext[] ciphertexts) {
		try {
			byte[] cipherBytes = Serializer.serialize(ciphertexts);
			signer.update(cipherBytes);
			return signer.sign();
		} catch (IOException e) {
			System.err.println("Malformed TelecomCiphertext");
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Uses our keys to sign some telecom responses.
	 * @param response The telecom responses to sign.
	 * @return This party's signature on the responses.
	 */
	public byte[] sign(TelecomResponse[] responses) {
		try {
			byte[] cipherBytes = Serializer.serialize(responses);
			signer.update(cipherBytes);
			return signer.sign();
		} catch (IOException e) {
			System.err.println("Malformed TelecomResponse");
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Uses a party's key to verify a telecom ciphertext.
	 * @param signerId The id of the party who signed the ciphertext.
	 * @param signedTC The signed telecom ciphertext.
	 * @return True if the signature is present and verifies.
	 */
	public boolean verify(int signerId, SignedTelecomCiphertext signedTC) {
		Signature verifier = verifiers.get(signerId);
		byte[] signature = signedTC.getSignatures().get(signerId);
		if (signature == null) {
			return false;
		}
		try {
			verifier.update(Serializer.serialize(signedTC.getCiphertexts()));
			return verifier.verify(signature);
		} catch (SignatureException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean verify(SignedTelecomResponse signedTR) {
		Signature verifier = verifiers.get(signedTR.getTelecomId());
		byte[] signature = signedTR.getSignature();
		if (signature == null) {
			return false;
		}
		try {
			verifier.update(Serializer.serialize(signedTR.getTelecomResponses()));
			return verifier.verify(signature);
		} catch (SignatureException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Encrypts an integer into a telecom ciphertext.
	 * @param receiverId The telecom who will receive this ciphertext.
	 * @param data The integer to encrypt
	 * @return The encrypted data.
	 */
	public byte[] encrypt(int receiverId, int data) {
		byte[] byteData = BigInteger.valueOf(data).toByteArray();
		try {
			return encrypters.get(receiverId).doFinal(byteData);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
