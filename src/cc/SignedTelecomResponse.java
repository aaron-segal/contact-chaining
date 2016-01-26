package cc;

import java.io.Serializable;

/**
 * Wrapper around a set of TelecomResponses which includes a signature on the
 * enclosed data.
 * @author Aaron Segal
 */
public class SignedTelecomResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	private TelecomResponse[] telecomResponses;
	private int telecomId;
	private byte[] signature;

	public SignedTelecomResponse(TelecomResponse telecomResponse, int telecomId) {
		telecomResponses = new TelecomResponse[1];
		telecomResponses[0] = telecomResponse;
		this.telecomId = telecomId;
	}

	public SignedTelecomResponse(TelecomResponse[] telecomResponses, int telecomId) {
		this.telecomResponses = telecomResponses;
		this.telecomId = telecomId;
	}

	/**
	 * @return the telecomResponses
	 */
	public TelecomResponse[] getTelecomResponses() {
		return telecomResponses;
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
