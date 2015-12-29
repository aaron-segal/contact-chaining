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
		if (!oSocket.open) {
			System.err.println("Error: Oversight socket " + oSocket.agencyId + " is closed!");
			return;
		}
		try {
			// Send the signed telecom ciphertext to the oversight agency
			oSocket.outputStream.writeObject(stc);
			oSocket.outputStream.flush();
			// Read the new signature back
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
