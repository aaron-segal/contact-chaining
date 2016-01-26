package cc;

import java.io.IOException;
import java.util.HashMap;

public class OversightSearchThread extends Thread {

	private OversightSocket oSocket;
	private HashMap<Integer, SignedTelecomResponse> prevResponses;
	private HashMap<Integer, byte[]> signatures = null;

	public OversightSearchThread(OversightSocket oSocket,
			HashMap<Integer, SignedTelecomResponse> prevResponses) {
		this.oSocket = oSocket;
		this.prevResponses = prevResponses;
	}

	@SuppressWarnings("unchecked")
	public void run() {
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
			signatures = (HashMap<Integer, byte[]>) oSocket.inputStream.readObject();
		} catch (IOException e) {
			e.printStackTrace();
			oSocket.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			oSocket.close();
		}
	}

	public byte[] getSignature(int telecomId){
		return signatures.get(telecomId);
	}

	public int getAgencyId(){
		return oSocket.getAgencyId();
	}

}
