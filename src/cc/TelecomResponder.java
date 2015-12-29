package cc;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

public class TelecomResponder extends Thread {

	private SignedTelecomCiphertext signedTC;
	private ObjectOutputStream outputStream;
	private TelecomData data;
	private Keys keys;

	public TelecomResponder(SignedTelecomCiphertext signedTC, ObjectOutputStream outputStream, TelecomData data, Keys keys) {
		this.signedTC = signedTC;
		this.outputStream = outputStream;
		this.data = data;
		this.keys = keys;
		start();
	}

	private void sendResponse(TelecomResponse response) throws IOException {
		byte[] signature = keys.sign(response);
		outputStream.writeObject(response);
		outputStream.writeObject(signature);
		outputStream.flush();
	}

	public void run() {
		//handle request
		try {
			boolean signaturesVerify = true;
			// check to make sure all signatures verify
			for (int agencyId : keys.getAgencyIds()) {
				signaturesVerify &= keys.verify(agencyId, signedTC);
			}
			if (!signaturesVerify) {
				sendResponse(new TelecomResponse(TelecomResponse.MsgType.INVALID_SIGNATURE));
				return;
			}
			BigInteger queryId = keys.getPrivateKey().
					decrypt(signedTC.telecomCiphertext.getEncryptedId());
			TelecomResponse response = data.queryResponse(queryId.intValue(), keys);
			sendResponse(response);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


}
