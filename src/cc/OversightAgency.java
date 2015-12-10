package cc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class OversightAgency extends Agency {

	public static final int MAX_TRIES = 10;
	public static final long SLEEP_BETWEEN_TRIES = 1000;
	public static final String LEADER_IP = "LEADERIP";

	private Socket leaderSocket = null;
	private boolean connected = false;
	private ObjectOutputStream leaderOutputStream;
	private ObjectInputStream leaderInputStream;

	@Override
	protected void usage() {
		System.err.println("Usage: java cc.OversightAgency config_file [-k key_file] [-q] [-s]");
	}

	public OversightAgency(String[] args) {
		super(args);
		String[] address = config.getProperty(LEADER_IP).split(":");
		String leaderIp = address[0]; // ip
		int leaderPort = Integer.parseInt(address[1]); //port

		println("Attemping to connect to host " + leaderIp +
				" on port " + leaderPort + ".");

		connected = false;
		for (int i = 0; i < MAX_TRIES && !connected; i++) {
			try {
				leaderSocket = new Socket(leaderIp, leaderPort);
				leaderOutputStream = new ObjectOutputStream(leaderSocket.getOutputStream());
				leaderInputStream = new ObjectInputStream(leaderSocket.getInputStream());
				println("Connected!");
				connected = true;
			} catch (UnknownHostException e) {
				System.err.println("Don't know about host: " + leaderIp);
				return;
			} catch (IOException e) {
				System.err.println("Waiting for connection to " + leaderIp + ":" + leaderPort);
				try {
					Thread.sleep(SLEEP_BETWEEN_TRIES);
				} catch (InterruptedException f) {
					continue;
				}
			}
		}
		if (!connected) {
			System.err.println("Could not connect to " + leaderIp + ":" + leaderPort);
			return;
		}
	}

	public void go() throws IOException {
		leaderOutputStream.writeInt(id);
		leaderOutputStream.flush();
		int leaderTarget = leaderInputStream.readInt();
		if (leaderTarget != targetId) {
			println("Failure: Investgating agency gave target " + leaderTarget +
					" but out target is " + targetId + ".");
		}

	}

	public void close() {
		try {
			leaderOutputStream.close();
			leaderInputStream.close();
			leaderSocket.close();
		} catch (IOException e) {}
	}

	public static void main(String[] args) {
		OversightAgency oAgency = new OversightAgency(args);
		try {
			oAgency.go();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			oAgency.close();
		}
	}
}
