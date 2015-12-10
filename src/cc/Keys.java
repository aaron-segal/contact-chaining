package cc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
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
	private int[] agencyIds;

	public Keys(String privateKeyFilename, String publicKeyFilename, int[] agencyIds)
			throws IOException {
		loadPrivateKey(privateKeyFilename);
		loadPublicKeys(publicKeyFilename);
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
}
