package cc;

import java.io.IOException;

public class OversightSearchThread extends CPUTrackingThread {

	private OversightSocket oSocket;
	private SignedTelecomResponse[] prevResponses;
	private byte[][] signatures = null;

	public OversightSearchThread(OversightSocket oSocket,
			SignedTelecomResponse[] prevResponses) {
		super();
		this.oSocket = oSocket;
		this.prevResponses = prevResponses;
	}

	public void runReal() {
		if (!oSocket.open) {
			System.err.println("Error: Oversight socket " + oSocket.getAgencyId() + " is closed!");
			return;
		}
		try {
			// Send the oversight agency the response we got from the previous
			// telecom so they can stay in sync with us.
			oSocket.writeObject(prevResponses);
			// If the oversight agency is in sync with us, they will know what our
			// final queries should look like, and sign them.
			signatures = (byte[][]) oSocket.readObject();
		} catch (IOException e) {
			e.printStackTrace();
			oSocket.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			oSocket.close();
		}
	}

	public byte[] getSignature(int telecomId){
		return signatures[telecomId];
	}

	public int getAgencyId(){
		return oSocket.getAgencyId();
	}

}
