package cc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TelecomSocket {

	public Socket socket;
	public ObjectInputStream inputStream;
	public ObjectOutputStream outputStream;
	public boolean open = false;

	public TelecomSocket(Socket socket) throws IOException {
		this.socket = socket;
		inputStream = new ObjectInputStream(socket.getInputStream());
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		open = true;
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
