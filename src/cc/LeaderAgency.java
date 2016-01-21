package cc;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
		System.err.println("Usage: java cc.LeaderAgency config_file [-c config_file] [-d max_degree] [-l max_length] [-k private_key_file] [-q] [-s]");
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

	public void contactChaining() {
		super.contactChaining();
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
		byte[] initialTCT = keys.encrypt(initialOwner, targetId);
		TelecomCiphertext startTCT = new TelecomCiphertext(initialTCT, initialOwner);

		// Search on the first telecom ciphertext. It is different from the others
		// because it does not have a telecom signature.
		QueueTCT queueNext = new QueueTCT(startTCT, maxDistance);
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
			TelecomResponse telecomResponse = prevResponse.getTelecomResponse();
			if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.SEARCH_DATA) {
				agencyCiphertexts.add(telecomResponse.getAgencyCiphertext());
				if (maxDistance > 0) {
					for (TelecomCiphertext tc :
						prevResponse.getTelecomResponse().getTelecomCiphertexts()) {
						investigationQueue.addLast(new QueueTCT(tc, maxDistance - 1));
					}
					println(prevResponse.getTelecomResponse().
							getTelecomCiphertexts().length + " added to queue");
				}
				// Store degree of target for timing data
				targetDegree = prevResponse.getTelecomResponse().
						getTelecomCiphertexts().length;
			} else {
				//println("MsgType: " + signedTR.telecomResponse.getMsgType());
				System.err.println("Cannot continue; got initial MsgType " +
						prevResponse.getTelecomResponse().getMsgType());
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

		// Remember that we still need to tell the first telecom about maxDegree.
		// It is already in the list of known telecoms so we won't do this otherwise.
		boolean needToInformInitialOwner = true;

		// Setup containers for final queries.
		finalCiphertexts.put(initialOwner, new ArrayList<TelecomCiphertext>());

		// We have the first responses we need to start investigating the graph.
		// We are ready to enter the main loop.
		while (!investigationQueue.isEmpty() && investigationQueue.peek().distance > 0) {
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
				nextSignedTC.setMaxDegree(maxDegree);
			} else if (needToInformInitialOwner && nextOwner == initialOwner) {
				nextSignedTC.setMaxDegree(maxDegree);
				needToInformInitialOwner = false;
			}
			try {
				ObjectOutputStream oos = telecoms.get(nextOwner).outputStream;
				ObjectInputStream ois = telecoms.get(nextOwner).inputStream;
				oos.writeObject(nextSignedTC);
				oos.flush();
				prevResponse = (SignedTelecomResponse) ois.readObject();
				TelecomResponse telecomResponse = prevResponse.getTelecomResponse();
				if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.SEARCH_DATA) {
					agencyCiphertexts.add(telecomResponse.getAgencyCiphertext());
					if (queueNext.distance < 1 && telecomResponse.getTelecomCiphertexts().length > 0) {
						println(telecomResponse.getTelecomCiphertexts().length +
								" not added to queue; maximum path length reached");
					} else if (queueNext.distance < 1) {
						println("No new ciphertexts added to queue; maximum path length reached");
					} else if (telecomResponse.getTelecomCiphertexts().length > maxDegree){
						println(telecomResponse.getTelecomCiphertexts().length +
								" not added to queue; exceeds maximum degree");
					} else if (queueNext.distance == 1) {
						// Add to final query sets, not investigation queue.
						for (TelecomCiphertext tc : telecomResponse.getTelecomCiphertexts()) {
							if (!finalCiphertexts.containsKey(tc.getOwner())) {
								finalCiphertexts.put(tc.getOwner(), new ArrayList<TelecomCiphertext>());
							}
							finalCiphertexts.get(tc.getOwner()).add(tc);
						}
						println(telecomResponse.getTelecomCiphertexts().length + 
								" added to leaf set");
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

		// From here on, we won't add anything new to the queue. We only care about
		// agency ciphertexts. So, we'll bundle all our requests together to save
		// on signatures.
		HashMap<Integer, SignedTelecomCiphertext> finalQueries =
				new HashMap<Integer, SignedTelecomCiphertext>(); 
		for (int telecomId : finalCiphertexts.keySet()) {
			TelecomCiphertext[] finalTCs =
					new TelecomCiphertext[finalCiphertexts.get(telecomId).size()];
			finalCiphertexts.get(telecomId).toArray(finalTCs);
			finalQueries.put(telecomId, new SignedTelecomCiphertext(finalTCs));
			finalQueries.get(telecomId).addSignature(id, keys.sign(finalTCs));
		}

		OversightConcludeThread[] concludeThreads =
				new OversightConcludeThread[oversight.length];
		for (int i = 0; i < concludeThreads.length; i++) {
			concludeThreads[i] = new OversightConcludeThread(oversight[i], prevResponse);
			concludeThreads[i].start();
		}
		for (OversightConcludeThread oct : concludeThreads) {
			try {
				oct.join();
			} catch (InterruptedException e) {
			} finally {
				for (int telecomId : finalQueries.keySet()) {
					finalQueries.get(telecomId).addSignature(
							oct.getAgencyId(), oct.getSignature(telecomId));
				}
			}
		}
		for (int telecomId : finalQueries.keySet()) {
			println("Sending request for " +
					finalCiphertexts.get(telecomId).size() +
					" leaf nodes to telecom " + telecomId);
			if (!telecoms.containsKey(telecomId)) {
				connectTelecom(telecomId);
			}
			try {
				// This time, write all objects to telecoms before reading them
				ObjectOutputStream oos = telecoms.get(telecomId).outputStream;
				oos.writeObject(finalQueries.get(telecomId));
				oos.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		HashMap<Integer, SignedTelecomResponse> finalResponses =
				new HashMap<Integer, SignedTelecomResponse>();

		for (int telecomId : finalQueries.keySet()) {
			try {
				ObjectInputStream ois = telecoms.get(telecomId).inputStream;
				SignedTelecomResponse finalSTR =
						(SignedTelecomResponse) ois.readObject();
				finalResponses.put(telecomId, finalSTR);
				for (BigInteger[] agencyCiphertext :
					finalSTR.getTelecomResponse().getAgencyCiphertexts()) {
					agencyCiphertexts.add(agencyCiphertext);
				}
				println("Got " +
						finalSTR.getTelecomResponse().getAgencyCiphertexts().length +
						" additional agency ciphertexts from telecom " + telecomId);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return;
			}
		}

		// Finally, send the very last telecomResponses to our oversight agencies.
		OversightFinalResultsThread[] finalThreads =
				new OversightFinalResultsThread[oversight.length];
		for (int i = 0; i < finalThreads.length; i++) {
			finalThreads[i] = new OversightFinalResultsThread(oversight[i], finalResponses);
			finalThreads[i].start();
		}
		for (OversightFinalResultsThread ofrt : finalThreads) {
			try {
				ofrt.join();
			} catch (InterruptedException e) {
			} finally {
				if (!ofrt.concludeOK()) {
					System.err.println("Didn't get an OK from oversight agency " +
							ofrt.getAgencyId() + "!");
				}
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
		leaderAgency.contactChaining();
		leaderAgency.writeOutput();
		leaderAgency.reportTiming();
		leaderAgency.closeAll();
	}

}
