package cc;

import java.io.IOException;

public class OversightSearchThread extends Thread {
	
	private OversightSocket oSocket;
	private SignedTelecomResponse prevTR;
	private byte[] signature = null;
	
	public OversightSearchThread(OversightSocket oSocket,
			SignedTelecomResponse prevTR) {
		this.oSocket = oSocket;
		this.prevTR = prevTR;
	}
	
	public void run() {
		if (!oSocket.open) {
			System.err.println("Error: Oversight socket " + oSocket.agencyId + " is closed!");
			return;
		}
		try {
			// Send the oversight agency the response we got from the previous
			// telecom so they can stay in sync with us.
			oSocket.outputStream.writeObject(prevTR);
			oSocket.outputStream.flush();
			// If the oversight agency is in sync with us, they will know what our
			// next ciphertext is, and sign it.
			signature = (byte[]) oSocket.inputStream.readObject();
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
		return oSocket.agencyId;
	}
}