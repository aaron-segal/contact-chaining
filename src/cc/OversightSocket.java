package cc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class OversightSocket {

	public Socket socket;
	private int agencyId = Integer.MIN_VALUE;
	public ObjectInputStream inputStream;
	public ObjectOutputStream outputStream;
	public LeaderAgency lAgency;
	public boolean open = false;

	public OversightSocket(Socket socket, LeaderAgency lAgency) throws IOException {
		this.socket = socket;
		this.lAgency = lAgency;
		inputStream = new ObjectInputStream(socket.getInputStream());
		outputStream = new ObjectOutputStream(socket.getOutputStream());
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

	/**
	 * Gets the agency ID, but throws a RuntimeException if the agency ID has not been set yet.
	 * @return the agencyId, if it has been set.
	 */
	public int getAgencyId() {
		if (agencyId == Integer.MIN_VALUE) {
			throw new RuntimeException("Agency ID requested, but has not been sent yet!");
		}
		return agencyId;
	}

	/**
	 * @param agencyId the agencyId to set
	 */
	public void setAgencyId(int agencyId) {
		this.agencyId = agencyId;
	}

}
