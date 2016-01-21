package cc;

import java.math.BigInteger;
import java.security.GeneralSecurityException;

public class ConvertWorker extends Thread {

	private TelecomKeys keys;
	private int threadId;
	private TelecomData data;
	// Where in the array to start convert, and how many ciphertexts we should convert.
	private int startIndex, toConvert;

	public ConvertWorker(TelecomData data, int startIndex, int toConvert,
			TelecomKeys keys, int threadId) {
		this.data = data;
		this.startIndex = startIndex;
		this.toConvert = toConvert;
		this.keys = keys;
		this.threadId = threadId;
	}

	/**
	 * Decrypts TelecomCiphertexts to ints, and then re-encrypts to agency
	 * ciphertexts (BigInteger[]), starting from startIndex until either toEncrypt
	 * items have been converted, or the end of the array is reached. 
	 * Duplicate items will appear as null agency ciphertexts.
	 */
	public void run() {
		CommutativeElGamal commEncrypter = new CommutativeElGamal();
		for (int i = startIndex; i - startIndex < toConvert &&
				i < data.currentCiphertexts.length; i++) {
			int userId;
			try {
				userId = keys.decrypt(data.currentCiphertexts[i].getEncryptedId(),
						threadId);
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				return;
			}
			if (data.skipUser(userId)) {
				data.currentAgencyCiphertexts[i] = null;
				continue;
			}
			data.currentAgencyCiphertexts[i] = new BigInteger[1];
			data.currentAgencyCiphertexts[i][0] = BigInteger.valueOf(userId);
			for (int agencyId : keys.getAgencyIds()) {
				data.currentAgencyCiphertexts[i] = commEncrypter.encrypt(agencyId,
						keys.getAgencyPublicKey(agencyId),
						data.currentAgencyCiphertexts[i]);
			}
		}
	}

}
