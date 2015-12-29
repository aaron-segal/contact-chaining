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
import java.security.*;

public class SigningKeyGen {

	public static final String VERIFY_PREFIX = "verify_";
	public static final String SIGNING_PREFIX = "sign_";
	public static final String SUFFIX = ".key";

	public static void usage() {
		System.out.println("Usage: cc.SigningKeyGen path minId maxId");
	}
	public static void main(String[] args) {
		if (args.length != 3) {
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

			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			keyGen.initialize(1024, random);
			for (int i = 0; i < signingKeyFiles.length; i++) {
				KeyPair pair = keyGen.generateKeyPair();
				PrivateKey priv = pair.getPrivate();
				PublicKey pub = pair.getPublic();
				byte[] signBytes = priv.getEncoded();
				byte[] verBytes = pub.getEncoded();
				signingKeyFiles[i].write(signBytes);
				signingKeyFiles[i].close();
				verifyKeyFiles[i].write(verBytes);
				verifyKeyFiles[i].close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
