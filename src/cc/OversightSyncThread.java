package cc;

import java.io.IOException;

public class OversightSyncThread extends Thread {

	private OversightSocket oSocket;

	public OversightSyncThread(OversightSocket oSocket) {
		this.oSocket = oSocket;
	}

	public void run() {
		try {
			// SYN protocol: Learn agency's id, tell it targetId
			oSocket.agencyId = oSocket.inputStream.readInt();
			oSocket.lAgency.println("Read oversight agency id " + oSocket.agencyId);
			oSocket.outputStream.writeInt(oSocket.lAgency.getTargetId());
			oSocket.outputStream.flush();
			oSocket.open = true;
		} catch (IOException e) {
			e.printStackTrace();
			oSocket.open = false;
			oSocket.close();
		}
	}
}
