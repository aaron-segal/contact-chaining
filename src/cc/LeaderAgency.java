package cc;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Properties;

public class LeaderAgency extends Agency {

	public static final String PORT = "PORT";
	public static final String TELECOM_IPS = "TELECOMIPS";
	private int port;
	private ServerSocket agencySocket;
	private OversightSocket[] oversight;
	private ArrayDeque<QueueTCT> investigationQueue;
	private HashMap<Integer, TelecomSocket> telecoms;
	private Properties telecomIPs;

	private class QueueTCT {
		// The TelecomCiphertext to search for
		public TelecomCiphertext data;
		// The distance out from here we are willing to search
		public int distance;
		public QueueTCT() {}
		public QueueTCT(TelecomCiphertext data, int distance) {
			this.data = data;
			this.distance = distance;
		}
	}

	@Override
	protected void usage() {
		System.err.println("Usage: java cc.LeaderAgency config_file_ [-c config_file] [-k private_key_file] [-q] [-s]");
	}

	public LeaderAgency(String[] args) {
		super(args);
		telecomIPs = new Properties();
		try {
			FileReader telecomIPFile = new FileReader(config.getProperty(TELECOM_IPS));
			telecomIPs.load(telecomIPFile);
			telecomIPFile.close();
		} catch (IOException e) {
			System.err.println("Could not load telecom IP file " + config.getProperty(TELECOM_IPS));
			System.exit(1);
		} 
		investigationQueue = new ArrayDeque<QueueTCT>();
		port = Integer.parseInt(config.getProperty(PORT));
		try {
			agencySocket = new ServerSocket(port);
			println("IP:Host = " + "127.0.0.1" + ":" + port);
		} catch (IOException e) {
			System.err.println("Could not listen on port:" + port);
			return;
		}
		oversight = new OversightSocket[numAgencies-1];
		telecoms = new HashMap<Integer, TelecomSocket>();
	}

	private int waitForConnections() {
		int connected = 0;
		while (connected < numAgencies - 1) {
			try {
				Socket newConnection = agencySocket.accept();
				OversightSocket newSocket =
						new OversightSocket(newConnection, this);
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
		} else {
			println(connected + " agencies connected.");
		}

		// We are now all connected up. Now initialize the search with a
		// TelecomCiphertext representing the target.
		int initialOwner = DataGen.provider(targetId, numTelecoms);
		BigInteger[] initialTCT = new CommutativeElGamal().
				encrypt(keys.getPublicKey(initialOwner),
						BigInteger.valueOf(targetId));
		TelecomCiphertext startTCT = new TelecomCiphertext(initialTCT, initialOwner);
		investigationQueue.addLast(new QueueTCT(startTCT, maxDistance));

		// Search on the first telecom ciphertext. It is different from the others
		// because it does not have a telecom signature.
		QueueTCT queueNext = investigationQueue.pop();
		TelecomCiphertext tC = queueNext.data;
		SignedTelecomCiphertext signedTC = new SignedTelecomCiphertext(tC);
		signedTC.addSignature(id, keys.sign(tC));
		OversightFirstThread[] firstThreads = new OversightFirstThread[oversight.length];
		for (int i = 0; i < firstThreads.length; i++) {
			firstThreads[i] = new OversightFirstThread(oversight[i], signedTC);
			firstThreads[i].start();
		}
		for (OversightFirstThread oft : firstThreads) {
			try {
				oft.join();
			} catch (InterruptedException e) {
			} finally {
				signedTC.addSignature(oft.getAgencyId(), oft.getSignature());
			}
		}
		
		println("Got first request for telecom ready to go.");

		// Get response for the first telecom ciphertext. This is different from the
		// general loop because we will not care about the degree of this first
		// vertex.
		connectTelecom(initialOwner);
		try {
			ObjectOutputStream oos = telecoms.get(initialOwner).outputStream;
			ObjectInputStream ois = telecoms.get(initialOwner).inputStream;
			oos.writeObject(signedTC);
			oos.flush();
			TelecomResponse response = (TelecomResponse) ois.readObject();
			byte[] signature = (byte[]) ois.readObject();
			//Test
			boolean verifies = keys.verify(initialOwner, response, signature);
			println("Verifies: " + verifies);
			println("MsgType: " + response.getMsgType());
			println("New vertices found: " + response.getTelecomCiphertexts().length);
		} catch (IOException e) {
			System.err.println("Error in connection with telecom " + initialOwner);
			e.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}


	}

	/**
	 * Adds a connection to a telecom.
	 * @param telecomId The id of the telecom to connect to.
	 * @return True if the connection was succesfully made; false else
	 */
	private boolean connectTelecom(int telecomId) {
		Socket tSocket;
		String[] address = telecomIPs.getProperty(""+telecomId).split(":");
		String telecomIp = address[0];
		int telecomPort = Integer.parseInt(address[1]);
		for (int i = 0; i < MAX_TRIES; i++) {
			try {
				tSocket = new Socket(telecomIp, telecomPort);
				telecoms.put(telecomId, new TelecomSocket(tSocket));
				println("Connected to telecom " + telecomId);
				return true;
			} catch (UnknownHostException e) {
				System.err.println("Don't know about host: " + telecomIp);
				return false;
			} catch (IOException e) {
				System.err.println("Waiting for connection to " + telecomIp + ":" + telecomPort);
				try {
					Thread.sleep(SLEEP_BETWEEN_TRIES);
				} catch (InterruptedException f) {
					continue;
				}
			}
		}
		System.err.println("Could not connect to " + telecomIp + ":" + telecomPort);
		return false;
	}

	public void closeAll() {
		for (OversightSocket os : oversight) {
			os.close();
		}
		for (TelecomSocket ts : telecoms.values()) {
			ts.close();
		}
	}

	public static void main(String[] args) {
		LeaderAgency leaderAgency = new LeaderAgency(args);
		leaderAgency.go();
		leaderAgency.closeAll();
	}

}
