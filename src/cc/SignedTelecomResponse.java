package cc;

import java.io.Serializable;

public class SignedTelecomResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	public TelecomResponse telecomResponse;
	public int telecomId;
	public byte[] signature;

	public SignedTelecomResponse() {}

	public SignedTelecomResponse(TelecomResponse telecomResponse, int telecomId) {
		this.telecomResponse = telecomResponse;
		this.telecomId = telecomId;
	}
}
