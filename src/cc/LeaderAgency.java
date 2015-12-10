package cc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class LeaderAgency extends Agency {

	public static final String PORT = "PORT";
	public static final String TELECOM_IPS = "TELECOMIPS";
	private int port;
	private ServerSocket agencySocket;
	private OversightSocket[] oversight;

	@Override
	protected void usage() {
		System.err.println("Usage: java cc.LeaderAgency config_file [-k key_file] [-q] [-s]");
	}

	public LeaderAgency(String[] args) {
		super(args);
		port = Integer.parseInt(config.getProperty(PORT));
		try {
			agencySocket = new ServerSocket(port);
			println("IP:Host = " + "127.0.0.1" + ":" + port);
		} catch (IOException e) {
			System.err.println("Could not listen on port:" + port);
			return;
		}
		oversight = new OversightSocket[numAgencies-1];
	}
	
	private int waitForConnections() {
		int connected = 0;
		while (connected < numAgencies - 1) {
			try {
				Socket newConnection = agencySocket.accept();
				OversightSocket newSocket =
						new OversightSocket(newConnection, targetId);
				oversight[connected] = newSocket;
				connected++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return connected;
	}
	
	public void go() {
		int connected = waitForConnections();
		if (connected < numAgencies - 1) {
			System.err.println("Only got " + connected + " agencies connected.");
			return;
		}
	}

	public static void main(String[] args) {
		LeaderAgency leaderAgency = new LeaderAgency(args);
		leaderAgency.go();
	}

}
