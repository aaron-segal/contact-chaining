package cc;

import java.io.IOException;

public class OversightFirstThread extends Thread {

	private OversightSocket oSocket;
	private SignedTelecomCiphertext stc;
	private byte[] signature;

	public OversightFirstThread(OversightSocket oSocket,
			SignedTelecomCiphertext stc) {
		this.oSocket = oSocket;
		this.stc = stc;
	}

	public void run() {
		try {
			// SYN protocol: Learn agency's id, tell it targetId
			oSocket.setAgencyId(oSocket.readInt());
			oSocket.lAgency.println("Read oversight agency id " + oSocket.getAgencyId());
			oSocket.writeInt(oSocket.lAgency.getTargetId());
			oSocket.open = true;

			// Send the signed telecom ciphertext to the oversight agency
			oSocket.writeObject(stc);

			// Read the new signature back
			signature = (byte[]) oSocket.readObject();
		} catch (IOException e) {
			e.printStackTrace();
			oSocket.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			oSocket.close();
		}
	}

	public byte[] getSignature(){
		return signature;
	}

	public int getAgencyId(){
		return oSocket.getAgencyId();
	}
}
