package nocrypto;

import java.io.Serializable;

/**
 * This class contains all the data that the telecoms include when replying to
 * queries by the agencies.
 * @author Aaron Segal
 *
 */

public class TelecomResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum MsgType {
		ERROR, DATA, NOT_FOUND, ALREADY_SENT
	}

	private MsgType msgType;
	private int agencyUserId;
	private TelecomRecord[] telecomRecords;

	public TelecomResponse(int agencyUserId, TelecomRecord[] telecomRecords) {
		this.agencyUserId = agencyUserId;
		this.telecomRecords = telecomRecords;
		setMsgType(MsgType.DATA);
	}

	public TelecomResponse(int agencyUserId) {
		this.agencyUserId = agencyUserId;
		this.telecomRecords = null;
		setMsgType(MsgType.DATA);
	}

	public TelecomResponse(MsgType msgType) {
		agencyUserId = -1;
		telecomRecords = null;
		this.setMsgType(msgType);
	}

	/**
	 * @return the agencyUserId
	 */
	public int getAgencyUserId() {
		return agencyUserId;
	}

	/**
	 * @return the telecomCiphertexts
	 */
	public TelecomRecord[] getTelecomRecords() {
		return telecomRecords;
	}

	/**
	 * @param telecomRecords the telecomRecords to set
	 */
	public void setTelecomRecords(TelecomRecord[] telecomRecords) {
		this.telecomRecords = telecomRecords;
	}

	/**
	 * @return the msgType
	 */
	public MsgType getMsgType() {
		return msgType;
	}

	/**
	 * @param msgType the msgType to set
	 */
	public void setMsgType(MsgType msgType) {
		this.msgType = msgType;
	}

}
