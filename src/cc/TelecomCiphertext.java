package cc;

import java.io.Serializable;

/**
 * A ciphertext to be decrypted by a telecom.
 * Agencies pass this on and should learn nothing by inspecting it.
 */
public class TelecomCiphertext implements Serializable {
	private static final long serialVersionUID = 1L;
	private byte[] encryptedId;
	private int owner;

	public TelecomCiphertext() {
	}

	public TelecomCiphertext(byte[] encryptedId, int owner) {
		this.encryptedId = encryptedId;
		this.owner = owner;
	}

	/**
	 * @return the encryptedId
	 */
	public byte[] getEncryptedId() {
		return encryptedId;
	}
	/**
	 * @param encryptedId the encryptedId to set
	 */
	public void setEncryptedId(byte[] encryptedId) {
		this.encryptedId = encryptedId;
	}
	/**
	 * @return the owner
	 */
	public int getOwner() {
		return owner;
	}
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(int owner) {
		this.owner = owner;
	}
}
