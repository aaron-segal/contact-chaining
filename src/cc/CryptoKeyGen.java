package cc;

/*
 * Generates signing and verification keys for a number of users.
 * Each user has an id from idMin to idMax inclusive.
 * Each key is stored as path/verify_X.key or path/sign_X.key, where X is the id.
 * 
 * For example, if idMin=1 and idMax=2, this generates 4 files:
 * path/sign_1.key
 * path/sign_2.key
 * path/verify_1.key
 * path/verify_2.key
 * 
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

public class CryptoKeyGen {

	public static final String PUBLIC_PREFIX = "pub_";
	public static final String PRIVATE_PREFIX = "priv_";
	public static final String VERIFY_PREFIX = "verify_";
	public static final String SIGNING_PREFIX = "sign_";
	public static final String SUFFIX = ".key";
	public static final String ENCRYPTION_ALGORITHM = "RSA";
	public static final String SIGNING_ALGORITHM = "DSA";
	public static final int KEY_SIZE = 2048;

	public static void usage() {
		System.out.println("Usage: cc.CryptoKeyGen path minId maxId [-e] [-s]");
		System.out.println("-e\t to generate encryption/decryption keys");
		System.out.println("-s\t to generate signing/verification keys");
	}

	public static void generateKeys(String algorithm,
			FileOutputStream[] publicKeyFiles, FileOutputStream[] privateKeyFiles)
					throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		keyGen.initialize(KEY_SIZE, random);
		for (int i = 0; i < privateKeyFiles.length; i++) {
			KeyPair pair = keyGen.generateKeyPair();
			PrivateKey priv = pair.getPrivate();
			PublicKey pub = pair.getPublic();
			byte[] signBytes = priv.getEncoded();
			byte[] verBytes = pub.getEncoded();
			privateKeyFiles[i].write(signBytes);
			privateKeyFiles[i].close();
			publicKeyFiles[i].write(verBytes);
			publicKeyFiles[i].close();
		}
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			usage();
			return;
		}
		String path = args[0];
		int minId = Integer.parseInt(args[1]);
		int maxId = Integer.parseInt(args[2]);
		if (minId > maxId) {
			usage();
			return;
		}
		boolean encryptionWanted = false;
		boolean signingWanted = false;
		for (int i = 3; i < args.length; i++) {
			if (args[i].equals("-e")) {
				encryptionWanted = true;
			} else if (args[i].equals("-s")) {
				signingWanted = true;
			}
		}

		if (!(encryptionWanted || signingWanted)) {
			System.out.println("Please provide -e for encryption keys or -s for signing keys.");
			usage();
			return;
		}

		if (encryptionWanted) {
			try {
				FileOutputStream[] publicKeyFiles =
						new FileOutputStream[maxId - minId + 1];
				FileOutputStream[] privateKeyFiles =
						new FileOutputStream[maxId - minId + 1];
				for (int i = 0; i < privateKeyFiles.length; i++) {
					publicKeyFiles[i] = new FileOutputStream (path + File.separator + 
							PUBLIC_PREFIX + (i + minId) + SUFFIX);
					privateKeyFiles[i] = new FileOutputStream (path + File.separator + 
							PRIVATE_PREFIX + (i + minId) + SUFFIX);
				}
				generateKeys(ENCRYPTION_ALGORITHM, publicKeyFiles, privateKeyFiles);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (signingWanted) {
			try {
				FileOutputStream[] verifyKeyFiles =
						new FileOutputStream[maxId - minId + 1];
				FileOutputStream[] signingKeyFiles =
						new FileOutputStream[maxId - minId + 1];
				for (int i = 0; i < signingKeyFiles.length; i++) {
					verifyKeyFiles[i] = new FileOutputStream (path + File.separator + 
							VERIFY_PREFIX + (i + minId) + SUFFIX);
					signingKeyFiles[i] = new FileOutputStream (path + File.separator + 
							SIGNING_PREFIX + (i + minId) + SUFFIX);
				}
				generateKeys(SIGNING_ALGORITHM, verifyKeyFiles, signingKeyFiles);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
