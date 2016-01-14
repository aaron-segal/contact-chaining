package cc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class OversightSocket {

	public Socket socket;
	public int agencyId;
	public ObjectInputStream inputStream;
	public ObjectOutputStream outputStream;
	public LeaderAgency lAgency;
	public boolean open = false;

	public OversightSocket(Socket socket, LeaderAgency lAgency) throws IOException {
		this.socket = socket;
		this.lAgency = lAgency;
		inputStream = new ObjectInputStream(socket.getInputStream());
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		OversightSyncThread ost = new OversightSyncThread(this);
		ost.start();
	}

	public void close() {
		if (!open) {
			return;
		}
		try {
			inputStream.close();
			outputStream.close();
			socket.close();
		} catch (IOException e) {}
		open = false;
	}

}
