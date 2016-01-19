package cc;

public class EncryptWorker extends Thread {

	private TelecomKeys keys;
	private int threadId;
	private TelecomData data;
	// Where in the array to start encrypting, and how many plaintexts we should encrypt.
	private int startIndex, toEncrypt;

	public EncryptWorker(TelecomData data, int startIndex, int toEncrypt,
			TelecomKeys keys, int threadId) {
		this.data = data;
		this.startIndex = startIndex;
		this.toEncrypt = toEncrypt;
		this.keys = keys;
		this.threadId = threadId;
	}

	/**
	 * Encrypts starting from startIndex until either toEncrypt items have been
	 * encrypted, or the end of the array is reached. 
	 */
	public void run() {
		for (int i = startIndex; i - startIndex < toEncrypt &&
				i < data.currentPlaintexts.length; i++) {
			int owner = DataGen.provider(data.currentPlaintexts[i],
					data.getNumTelecoms());
			data.currentCiphertexts[i] = new TelecomCiphertext();
			data.currentCiphertexts[i].setOwner(owner);
			data.currentCiphertexts[i].setEncryptedId(keys.encrypt(owner,
					data.currentPlaintexts[i], threadId));
		}
	}

}
