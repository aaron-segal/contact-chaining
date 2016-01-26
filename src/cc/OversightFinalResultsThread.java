package cc;

import java.io.IOException;
import java.util.HashMap;

public class OversightFinalResultsThread extends Thread {

	private OversightSocket oSocket;
	private HashMap<Integer, SignedTelecomResponse> prevResponses;
	private boolean concludeOK = false;

	public OversightFinalResultsThread(OversightSocket oSocket,
			HashMap<Integer, SignedTelecomResponse> prevResponses) {
		this.oSocket = oSocket;
		this.prevResponses = prevResponses;
	}

	public void run() {
		if (!oSocket.open) {
			System.err.println("Error: Oversight socket " + oSocket.getAgencyId() + " is closed!");
			return;
		}
		try {
			// Send the oversight agency the response we got from the previous
			// telecom so they can stay in sync with us.
			oSocket.writeObject(prevResponses);
			// If the everything has gone perfect, the oversight agency should send
			// us a True.
			concludeOK = (boolean) oSocket.inputStream.readObject();
		} catch (IOException e) {
			e.printStackTrace();
			oSocket.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			oSocket.close();
		}
	}

	public boolean concludeOK(){
		return concludeOK;
	}

	public int getAgencyId(){
		return oSocket.getAgencyId();
	}

}
