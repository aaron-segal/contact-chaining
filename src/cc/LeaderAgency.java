package cc;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;

import cc.TelecomResponse.MsgType;

public class LeaderAgency extends Agency {

	public static final String PORT = "PORT";
	public static final String TELECOM_IPS = "TELECOMIPS";
	private int port;
	private Properties telecomIPs;
	private ServerSocket agencySocket;
	private OversightSocket[] oversight;
	private HashMap<Integer, TelecomSocket> telecoms;


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
		println("Remaining in queue: " + investigationQueue.size());
		TelecomCiphertext nextTC = queueNext.data;
		SignedTelecomCiphertext nextSignedTC = new SignedTelecomCiphertext(nextTC);
		nextSignedTC.addSignature(id, keys.sign(nextTC));
		OversightFirstThread[] firstThreads = new OversightFirstThread[oversight.length];
		for (int i = 0; i < firstThreads.length; i++) {
			firstThreads[i] = new OversightFirstThread(oversight[i], nextSignedTC);
			firstThreads[i].start();
		}
		for (OversightFirstThread oft : firstThreads) {
			try {
				oft.join();
			} catch (InterruptedException e) {
			} finally {
				nextSignedTC.addSignature(oft.getAgencyId(), oft.getSignature());
			}
		}

		println("Got first request for telecom ready to go.");

		// Get response for the first telecom ciphertext. This is different from the
		// general loop because we will not care about the degree of this first
		// vertex.
		connectTelecom(initialOwner);
		SignedTelecomResponse prevResponse;
		try {
			ObjectOutputStream oos = telecoms.get(initialOwner).outputStream;
			ObjectInputStream ois = telecoms.get(initialOwner).inputStream;
			oos.writeObject(nextSignedTC);
			oos.flush();
			prevResponse = (SignedTelecomResponse) ois.readObject();
			TelecomResponse telecomResponse = prevResponse.telecomResponse;
			if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.DATA) {
				agencyCiphertexts.add(telecomResponse.getAgencyCiphertext());
				if (maxDistance > 0) {
					for (TelecomCiphertext tc : prevResponse.telecomResponse.getTelecomCiphertexts()) {
						investigationQueue.addLast(new QueueTCT(tc, maxDistance - 1));
					}
					println(prevResponse.telecomResponse.getTelecomCiphertexts().length + 
							" added to queue");
				}
			} else {
				//println("MsgType: " + signedTR.telecomResponse.getMsgType());
				System.err.println("Cannot continue; got initial MsgType " + prevResponse.telecomResponse.getMsgType());
				return;
			}
		} catch (IOException e) {
			System.err.println("Error in connection with telecom " + initialOwner);
			e.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// We have the first responses we need to start investigating the graph.
		// We are ready to enter the main loop proper
		while (!investigationQueue.isEmpty()) {
			queueNext = investigationQueue.pop();
			println("Remaining in queue: " + investigationQueue.size());
			nextTC = queueNext.data;
			nextSignedTC = new SignedTelecomCiphertext(nextTC);
			nextSignedTC.addSignature(id, keys.sign(nextTC));

			// Get signatures from other oversight agencies
			OversightSearchThread[] searchThreads = new OversightSearchThread[oversight.length];
			for (int i = 0; i < searchThreads.length; i++) {
				searchThreads[i] = new OversightSearchThread(oversight[i], prevResponse);
				searchThreads[i].start();
			}
			for (OversightSearchThread ost : searchThreads) {
				try {
					ost.join();
				} catch (InterruptedException e) {
				} finally {
					nextSignedTC.addSignature(ost.getAgencyId(), ost.getSignature());
				}
			}

			// Send request to telecom and get response back
			int nextOwner = nextTC.getOwner();
			println("Sending signed request to telecom " + nextOwner + "...");
			if (!telecoms.containsKey(nextOwner)) {
				connectTelecom(nextOwner);
			}
			try {
				ObjectOutputStream oos = telecoms.get(nextOwner).outputStream;
				ObjectInputStream ois = telecoms.get(nextOwner).inputStream;
				oos.writeObject(nextSignedTC);
				oos.flush();
				prevResponse = (SignedTelecomResponse) ois.readObject();
				TelecomResponse telecomResponse = prevResponse.telecomResponse;
				if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.DATA) {
					agencyCiphertexts.add(telecomResponse.getAgencyCiphertext());
					if (queueNext.distance < 1) {
						println(telecomResponse.getTelecomCiphertexts().length +
								" not added to queue; maximum path length reached");
					} else if (telecomResponse.getTelecomCiphertexts().length > maxDegree){
						println(telecomResponse.getTelecomCiphertexts().length +
								" not added to queue; exceeds maximum degree");
					} else {
						for (TelecomCiphertext tc : telecomResponse.getTelecomCiphertexts()) {
							investigationQueue.addLast(new QueueTCT(tc, queueNext.distance - 1));
						}
						println(telecomResponse.getTelecomCiphertexts().length + 
								" added to queue");
					}
				} else if (telecomResponse.getMsgType() == MsgType.ALREADY_SENT) {
					println("MsgType: " + telecomResponse.getMsgType());
				} else {
					System.err.println("Error: MsgType: " + telecomResponse.getMsgType());
					return;
				}
			} catch (IOException e) {
				System.err.println("Error in connection with telecom " + nextOwner);
				e.printStackTrace();
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return;
			}
		}

		// Finally, send the very last telecomResponse to our oversight agencies.
		OversightSearchThread[] searchThreads = new OversightSearchThread[oversight.length];
		for (int i = 0; i < searchThreads.length; i++) {
			searchThreads[i] = new OversightSearchThread(oversight[i], prevResponse);
			searchThreads[i].start();
		}
		for (OversightSearchThread ost : searchThreads) {
			try {
				ost.join();
			} catch (InterruptedException e) {
			}
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
		leaderAgency.writeOutput();
		leaderAgency.closeAll();
	}

}
