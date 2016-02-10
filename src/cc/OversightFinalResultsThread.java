package cc;

import java.io.IOException;
import java.util.HashMap;

public class OversightFinalResultsThread extends CPUTrackingThread {

	private OversightSocket oSocket;
	private HashMap<Integer, SignedTelecomResponse> prevResponses;
	private long oversightCpuTime = -1;

	public OversightFinalResultsThread(OversightSocket oSocket,
			HashMap<Integer, SignedTelecomResponse> prevResponses) {
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
			// If the everything has gone perfect, the oversight agency should send
			// us a True.
			oversightCpuTime = oSocket.readLong();
		} catch (IOException e) {
			e.printStackTrace();
			oSocket.close();
		}
	}

	public long getOversightCpuTime(){
		return oversightCpuTime;
	}

	public int getAgencyId(){
		return oSocket.getAgencyId();
	}

}
