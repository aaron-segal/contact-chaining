package cc;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * This class contains all the data that the telecoms include when replying to
 * queries by the agencies. All ids included in this data must be encrypted.
 * @author Aaron Segal
 *
 */

public class TelecomResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum MsgType {
		DATA, NOT_FOUND, ALREADY_SENT
	}
	
	private MsgType msgType;
	private BigInteger[] agencyCiphertext;
	private TelecomCiphertext[] telecomCiphertexts;
	
	public TelecomResponse(BigInteger[] agencyCiphertext, TelecomCiphertext[] telecomCiphertexts) {
		this.agencyCiphertext = agencyCiphertext;
		this.telecomCiphertexts = telecomCiphertexts;
		setMsgType(MsgType.DATA);
	}

	public TelecomResponse(MsgType msgType) {
		agencyCiphertext = null;
		telecomCiphertexts = null;
		this.setMsgType(msgType);
	}
	
	/**
	 * @return the agencyCiphertext
	 */
	public BigInteger[] getAgencyCiphertext() {
		return agencyCiphertext;
	}

	/**
	 * @param agencyCiphertext the agencyCiphertext to set
	 */
	public void setAgencyCiphertext(BigInteger[] agencyCiphertext) {
		this.agencyCiphertext = agencyCiphertext;
	}

	/**
	 * @return the telecomCiphertexts
	 */
	public TelecomCiphertext[] getTelecomCiphertexts() {
		return telecomCiphertexts;
	}

	/**
	 * @param telecomCiphertexts the telecomCiphertexts to set
	 */
	public void setTelecomCiphertexts(TelecomCiphertext[] telecomCiphertexts) {
		this.telecomCiphertexts = telecomCiphertexts;
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
