package nocrypto;

import java.io.Serializable;

/**
 * A plaintext value holding information about a user from a telecom.
 */
public class TelecomRecord implements Serializable {
	private static final long serialVersionUID = 1L;
	private int userId;
	private int owner;

	public TelecomRecord() {
	}

	public TelecomRecord(int userId, int owner) {
		this.userId = userId;
		this.owner = owner;
	}

	/**
	 * @return the userId
	 */
	public int getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(int userId) {
		this.userId = userId;
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
