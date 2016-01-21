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
		ERROR, SEARCH_DATA, CONCLUDE_DATA, NOT_FOUND, ALREADY_SENT, INVALID_SIGNATURE
	}

	private MsgType msgType;
	private BigInteger[][] agencyCiphertexts;
	private TelecomCiphertext[] telecomCiphertexts;

	public TelecomResponse(BigInteger[] agencyCiphertext, TelecomCiphertext[] telecomCiphertexts) {
		agencyCiphertexts = new BigInteger[1][];
		agencyCiphertexts[0] = agencyCiphertext;
		this.telecomCiphertexts = telecomCiphertexts;
		setMsgType(MsgType.SEARCH_DATA);
	}

	public TelecomResponse(BigInteger[][] agencyCiphertexts) {
		this.agencyCiphertexts = agencyCiphertexts;
		telecomCiphertexts = null;
		setMsgType(MsgType.CONCLUDE_DATA);
	}

	public TelecomResponse(MsgType msgType) {
		agencyCiphertexts = null;
		telecomCiphertexts = null;
		this.setMsgType(msgType);
	}

	/**
	 * To be used for SEARCH_DATA only. Throws RuntimeException else.
	 * @return the agencyCiphertext
	 */
	public BigInteger[] getAgencyCiphertext() {
		if (msgType != MsgType.SEARCH_DATA) {
			throw new RuntimeException("Called getAgencyCiphertext but MsgType was " +
					msgType + "!");
		}
		return agencyCiphertexts[0];
	}

	/**
	 * To be used for CONCLUDE_DATA only. Throws RuntimeException else.
	 * @return the agencyCiphertexts
	 */
	public BigInteger[][] getAgencyCiphertexts() {
		if (msgType != MsgType.CONCLUDE_DATA) {
			throw new RuntimeException("Called getAgencyCiphertexts but MsgType was " +
					msgType + "!");
		}
		return agencyCiphertexts;
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
