package cc;

import java.io.Serializable;
import java.math.BigInteger;


/**
 * A ciphertext to be decrypted by a telecom.
 * Agencies pass this on and should learn nothing by inspecting it.
 */
public class TelecomCiphertext implements Serializable {
	private static final long serialVersionUID = 1L;
	private BigInteger[] encryptedId;
	private int owner;
	/**
	 * @return the encryptedId
	 */
	public BigInteger[] getEncryptedId() {
		return encryptedId;
	}
	/**
	 * @param encryptedId the encryptedId to set
	 */
	public void setEncryptedId(BigInteger[] encryptedId) {
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
