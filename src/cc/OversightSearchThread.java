package cc;

import java.io.IOException;

public class OversightSearchThread extends Thread {
	
	private OversightSocket oSocket;
	private SignedTelecomCiphertext nextTC;
	private SignedTelecomResponse prevTR;
	private byte[] signature = null;
	
	public OversightSearchThread(OversightSocket oSocket,
			SignedTelecomResponse prevTR, SignedTelecomCiphertext nextTC) {
		this.oSocket = oSocket;
		this.nextTC = nextTC;
		this.prevTR = prevTR;
	}
	
	public void run() {
		if (!oSocket.open) {
			System.err.println("Error: Oversight socket " + oSocket.agencyId + " is closed!");
			return;
		}
		try {
			// Send the oversight agency the response we got from the previous
			// telecom so they can stay in sync with us, as well as the ciphertext
			// we propose to send next.
			oSocket.outputStream.writeObject(prevTR);
			oSocket.outputStream.writeObject(nextTC);
			oSocket.outputStream.flush();
			// If the oversight agency is in sync with us, they will agree to sign
			// our next ciphertext.
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
