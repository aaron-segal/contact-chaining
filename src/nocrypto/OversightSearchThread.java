package nocrypto;

import java.io.IOException;
import java.util.HashMap;

public class OversightSearchThread extends cc.CPUTrackingThread {

	private OversightSocket oSocket;
	private HashMap<Integer, BatchedTelecomResponse> prevResponses;
	private boolean isOkay;

	public OversightSearchThread(OversightSocket oSocket,
			HashMap<Integer, BatchedTelecomResponse> prevResponses) {
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
