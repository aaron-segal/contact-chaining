package cc;

import java.net.Socket;

public class TelecomResponder extends Thread {

	private Socket agencySocket;
	private TelecomData data;
	
	public TelecomResponder(Socket agencySocket, TelecomData data) {
		this.agencySocket = agencySocket;
		this.data = data;
	}
	
	public void run() {
		//handle request
	}
	
	
}
