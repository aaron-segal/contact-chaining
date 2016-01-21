package cc;

import java.io.Serializable;

/**
 * Wrapper around a TelecomResponse which includes a signature on the enclosed data.
 * @author Aaron Segal
 */
public class SignedTelecomResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	private TelecomResponse telecomResponse;
	private int telecomId;
	private byte[] signature;

	public SignedTelecomResponse(TelecomResponse telecomResponse, int telecomId) {
		this.telecomResponse = telecomResponse;
		this.telecomId = telecomId;
	}

	/**
	 * @return the telecomResponse
	 */
	public TelecomResponse getTelecomResponse() {
		return telecomResponse;
	}

	/**
	 * @return the signature
	 */
	public byte[] getSignature() {
		return signature;
	}

	/**
	 * @param signature the signature to set
	 */
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	/**
	 * @return the telecomId
	 */
	public int getTelecomId() {
		return telecomId;
	}

	/**
	 * @param telecomId the telecomId to set
	 */
	public void setTelecomId(int telecomId) {
		this.telecomId = telecomId;
	}

}
