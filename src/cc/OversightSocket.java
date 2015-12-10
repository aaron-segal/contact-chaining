package cc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class OversightSocket extends Thread {

	private Socket socket;
	private int agencyId;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private int targetId;
	
	public OversightSocket(Socket socket, int targetId) throws IOException {
		this.socket = socket;
		this.targetId = targetId;
		inputStream = new ObjectInputStream(socket.getInputStream());
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		start();
	}

	public void run() {
		try {
			agencyId = inputStream.readInt();
			outputStream.writeInt(targetId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the agencyId
	 */
	public int getAgencyId() {
		return agencyId;
	}

}
