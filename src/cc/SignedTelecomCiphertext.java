package cc;

import java.io.Serializable;
import java.util.HashMap;

public class SignedTelecomCiphertext implements Serializable {

	private static final long serialVersionUID = 1L;
	public TelecomCiphertext telecomCiphertext;
	public HashMap<Integer, byte[]> signatures;
	
	public SignedTelecomCiphertext() {
		signatures = new HashMap<Integer, byte[]>();
	}
	
	public SignedTelecomCiphertext(TelecomCiphertext telecomCiphertext) {
		this.telecomCiphertext = telecomCiphertext;
		signatures = new HashMap<Integer, byte[]>();
	}
	
	/**
	 * Adds a signature from the given id to this ciphertext.
	 * Multiple uses of this with the same id will overwrite the old signature.
	 * @param id
	 * @param signature
	 */
	public void addSignature(int id, byte[] signature) {
		signatures.put(id, signature);
	}
	
	public HashMap<Integer, byte[]> getSignatures() {
		return signatures;
	}
}
