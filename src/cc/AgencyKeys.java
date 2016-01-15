package cc;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Properties;

public class AgencyKeys extends Keys {

	private CommutativeElGamal privateKey;

	public AgencyKeys(String privateKeyFilename, String publicKeyFilename,
			String keysPath, int id, int[] agencyIds)
					throws IOException {
		super(privateKeyFilename, publicKeyFilename, keysPath, id, agencyIds);
		loadPrivateKey(privateKeyFilename);
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

	/**
	 * @return the privateKey
	 */
	public CommutativeElGamal getPrivateKey() {
		return privateKey;
	}

}
