package cc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Properties;
import java.util.Scanner;


/**
 * Stores one user's key data: a single private key, and many public keys.
 * @author Aaron Segal
 *
 */
public class Keys {
	private CommutativeElGamal privateKey;
	private HashMap<Integer, BigInteger> publicKeys;
	private PrivateKey signingKey;
	private Signature signer;
	private HashMap<Integer, PublicKey> verifyKeys;
	private HashMap<Integer, Signature> verifiers;
	private int[] agencyIds;
	private int id;

	public Keys(String privateKeyFilename, String publicKeyFilename,
			String signatureKeysPath, int id, int[] agencyIds)
					throws IOException {
		this.id = id;
		loadPrivateKey(privateKeyFilename);
		loadPublicKeys(publicKeyFilename);
		loadSignatureKeys(signatureKeysPath);
		this.agencyIds = agencyIds;
	}

	private void loadPrivateKey(String privateKeyFilename) throws IOException {
		FileReader pkreader= new FileReader(privateKeyFilename);
		Properties pkDefault = new Properties();
		pkDefault.setProperty(KeyGen.PRIME, ElGamal.prime1024.toString());
		pkDefault.setProperty(KeyGen.GENERATOR, ElGamal.generator1024.toString());
		Properties pk = new Properties(pkDefault);
		pk.load(pkreader);
		pkreader.close();
		int elgid = Integer.parseInt(pk.getProperty(KeyGen.ID));
		BigInteger privateKey = new BigInteger(pk.getProperty(KeyGen.PRIVATE_KEY));
		String primeString = pk.getProperty(KeyGen.PRIME);
		String genString = pk.getProperty(KeyGen.GENERATOR);
		if (primeString != null && genString != null) {
			this.privateKey = new CommutativeElGamal(elgid, new BigInteger(primeString), new BigInteger(genString), privateKey);
		} else {
			this.privateKey = new CommutativeElGamal(elgid, privateKey);
		}
	}

	private void loadPublicKeys(String publicKeyFilename) throws IOException {
		publicKeys = new HashMap<Integer, BigInteger>();
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
				publicKeys.put(currId, new BigInteger(lineParts[1]));
				currId = Integer.MIN_VALUE;
			}
		}
		spub.close(); 
	}

	private void loadSignatureKeys(String signatureKeysPath) throws IOException {
		verifyKeys = new HashMap<Integer,PublicKey>();
		verifiers = new HashMap<Integer, Signature>();
		File path = new File(signatureKeysPath);
		for (File keyFile : path.listFiles()) {
			if (keyFile.getName().equals(SigningKeyGen.SIGNING_PREFIX + id +
					SigningKeyGen.SUFFIX)) {
				// If this is our signing key, read it
				FileInputStream signingKeyInput = new FileInputStream(keyFile);
				byte[] signingKeyBytes = new byte[1024];
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
			} else if (keyFile.getName().startsWith(SigningKeyGen.VERIFY_PREFIX)) {
				// If this is a verification key, get its id
				String strippedFilename = keyFile.getName();
				strippedFilename = strippedFilename.replaceAll(SigningKeyGen.VERIFY_PREFIX,"");
				strippedFilename = strippedFilename.replaceAll(SigningKeyGen.SUFFIX,"");
				int keyId = Integer.parseInt(strippedFilename);
				// read it
				FileInputStream verificationKeyInput = new FileInputStream(keyFile);
				byte[] verificationKeyBytes = new byte[1024];
				int read = verificationKeyInput.read(verificationKeyBytes);
				verificationKeyBytes = Arrays.copyOf(verificationKeyBytes, read);
				verificationKeyInput.close();
				// and parse it as a verification key.
				X509EncodedKeySpec verKeySpec =
						new X509EncodedKeySpec(verificationKeyBytes);
				try {
					KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
					verifyKeys.put(keyId, keyFactory.generatePublic(verKeySpec));
					Signature verifier = Signature.getInstance("SHA1withDSA", "SUN");
					verifier.initVerify(verifyKeys.get(keyId));
					verifiers.put(keyId, verifier);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	/**
	 * @return the privateKey
	 */
	public CommutativeElGamal getPrivateKey() {
		return privateKey;
	}

	/**
	 * @param privateKey the privateKey to set
	 */
	public void setPrivateKey(CommutativeElGamal privateKey) {
		this.privateKey = privateKey;
	}

	public BigInteger getPublicKey(int keyId) {
		return publicKeys.get(keyId);
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
	 * Uses our keys to sign a telecom ciphertext.
	 * @param ciphertext The telecom ciphertext to sign.
	 * @return This party's signature on the ciphertext.
	 */
	public byte[] sign(TelecomCiphertext ciphertext) {
		try {
			byte[] cipherBytes = Serializer.serialize(ciphertext);
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
	 * Uses our keys to sign a telecom response.
	 * @param response The telecom response to sign.
	 * @return This party's signature on the response.
	 */
	public byte[] sign(TelecomResponse response) {
		try {
			byte[] cipherBytes = Serializer.serialize(response);
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
	 * @param id The id of the party who signed the ciphertext.
	 * @param signedTC The signed telecom ciphertext.
	 * @return True if the signature is present and verifies.
	 */
	public boolean verify(int id, SignedTelecomCiphertext signedTC) {
		Signature verifier = verifiers.get(id);
		byte[] signature = signedTC.signatures.get(id);
		if (signature == null) {
			return false;
		}
		try {
			verifier.update(Serializer.serialize(signedTC.telecomCiphertext));
			return verifier.verify(signature);
		} catch (SignatureException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean verify(int id, TelecomResponse response, byte[] signature) {
		Signature verifier = verifiers.get(id);
		if (signature == null) {
			return false;
		}
		try {
			verifier.update(Serializer.serialize(response));
			return verifier.verify(signature);
		} catch (SignatureException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
