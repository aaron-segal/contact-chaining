package cc;

import java.io.IOException;
import java.util.HashMap;

public class OversightConcludeThread extends Thread {

	private OversightSocket oSocket;
	private SignedTelecomResponse prevTR;
	private HashMap<Integer, byte[]> signatures = null;

	public OversightConcludeThread(OversightSocket oSocket,
			SignedTelecomResponse prevTR) {
		this.oSocket = oSocket;
		this.prevTR = prevTR;
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
			oSocket.outputStream.writeObject(prevTR);
			oSocket.outputStream.flush();
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
