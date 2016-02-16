package nocrypto;

import java.io.IOException;

public class OversightFirstThread extends cc.CPUTrackingThread {

	private OversightSocket oSocket;
	private BatchedTelecomRecord stc;
	private boolean isOkay;

	public OversightFirstThread(OversightSocket oSocket,
			BatchedTelecomRecord stc) {
		super();
		this.oSocket = oSocket;
		this.stc = stc;
	}

	public void runReal() {
		try {
			// SYN protocol: Learn agency's id, tell it targetId
			oSocket.setAgencyId(oSocket.readInt());
			oSocket.lAgency.println("Read oversight agency id " + oSocket.getAgencyId());
			oSocket.writeInt(oSocket.lAgency.getTargetId());
			oSocket.open = true;

			// Send the signed telecom ciphertext to the oversight agency
			oSocket.writeObject(stc);

			// Read the new signature back
			isOkay = (boolean) oSocket.readObject();
		} catch (IOException e) {
			e.printStackTrace();
			oSocket.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			oSocket.close();
		}
	}

	public boolean getOK(){
		return isOkay;
	}

	public int getAgencyId(){
		return oSocket.getAgencyId();
	}
}
