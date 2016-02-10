package cc;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import cc.SignedTelecomCiphertext.QueryType;

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
		System.err.println("Usage: java cc.LeaderAgency config_file [-c config_file] [-d max_degree] [-i id_of_target] [-l max_length] [-k private_key_file] [-o output_path] [-q] [-s]");
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

	private void writeObjectToTelecom(int telecomId, Object obj)
			throws IOException {
		ObjectOutputStream oos = telecoms.get(telecomId).outputStream;
		recordBytes(Serializer.objectSize(obj));
		oos.writeObject(obj);
		oos.flush();
		oos.reset();
	}

	private Object readObjectFromTelecom(int telecomId)
			throws ClassNotFoundException, IOException {
		ObjectInputStream ois = telecoms.get(telecomId).inputStream;
		Object obj = ois.readObject();
		recordBytes(Serializer.objectSize(obj));
		return obj;
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
		// Setup container for future queries.
		investigationLists.put(initialOwner, new ArrayList<TelecomCiphertext>());
		byte[] initialTCT = keys.encrypt(initialOwner, targetId);
		TelecomCiphertext startTCT = new TelecomCiphertext(initialTCT, initialOwner);

		// Search on the first telecom ciphertext. It is different from the others
		// because it does not have a telecom signature.
		SignedTelecomCiphertext firstSignedTC =	new SignedTelecomCiphertext(startTCT);
		firstSignedTC.addSignature(id, keys.sign(firstSignedTC.getCiphertexts()));
		OversightFirstThread[] firstThreads = new OversightFirstThread[oversight.length];
		for (int i = 0; i < firstThreads.length; i++) {
			firstThreads[i] = new OversightFirstThread(oversight[i], firstSignedTC);
			firstThreads[i].start();
		}
		for (OversightFirstThread oft : firstThreads) {
			try {
				oft.join();
			} catch (InterruptedException e) {
			} finally {
				firstSignedTC.addSignature(oft.getAgencyId(), oft.getSignature());
				recordAgencyCpuTime(oft.getCpuTime());
			}
		}

		println("Got first request for telecom ready to go.");

		// Get response for the first telecom ciphertext. This is different from the
		// general loop because we will not care about the degree of this first
		// vertex.
		connectTelecom(initialOwner);
		HashMap<Integer, SignedTelecomResponse> prevResponses =
				new HashMap<Integer, SignedTelecomResponse>();
		try {
			writeObjectToTelecom(initialOwner, firstSignedTC);
			SignedTelecomResponse firstSignedResponse =
					(SignedTelecomResponse) readObjectFromTelecom(initialOwner);
			prevResponses.put(initialOwner, firstSignedResponse);
			recordTelecomCpuTime(firstSignedResponse.getCpuTime());
			TelecomResponse telecomResponse = firstSignedResponse.getTelecomResponses()[0];
			if (telecomResponse.getMsgType() ==	TelecomResponse.MsgType.DATA) {
				processTelecomResponse(telecomResponse, 0);
				// Store degree of target for timing data
				targetDegree = telecomResponse.getTelecomCiphertexts().length;
			} else {
				System.err.println("Cannot continue; got initial MsgType " +
						telecomResponse.getMsgType());
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
		HashMap<Integer, SignedTelecomCiphertext> nextSignedTCs;

		// We have the first responses we need to start investigating the graph.
		// We are ready to enter the main loop.
		for (int distance = 1; distance <= maxDistance; distance++) {
			println("Remaining in queue: " + ciphertextsRemaining());
			nextSignedTCs = new HashMap<Integer, SignedTelecomCiphertext>();
			for (int telecomId : investigationLists.keySet()) {
				TelecomCiphertext[] ciphertexts = getCiphertexts(telecomId);
				nextSignedTCs.put(telecomId, new SignedTelecomCiphertext(ciphertexts));
				if (distance == maxDistance) {
					nextSignedTCs.get(telecomId).setType(QueryType.CONCLUDE);
				}
				nextSignedTCs.get(telecomId).addSignature(id, keys.sign(ciphertexts));
			}

			// Get signatures from other oversight agencies
			OversightSearchThread[] searchThreads = new OversightSearchThread[oversight.length];
			for (int i = 0; i < searchThreads.length; i++) {
				searchThreads[i] = new OversightSearchThread(oversight[i], prevResponses);
				searchThreads[i].start();
			}
			for (OversightSearchThread ost : searchThreads) {
				try {
					ost.join();
				} catch (InterruptedException e) {
				} finally {
					for (int telecomId : nextSignedTCs.keySet()) {
						nextSignedTCs.get(telecomId).addSignature(
								ost.getAgencyId(), ost.getSignature(telecomId));
					}
					recordAgencyCpuTime(ost.getCpuTime());
				}
			}

			// Send requests to telecoms
			for (int telecomId : nextSignedTCs.keySet()) {
				SignedTelecomCiphertext nextSignedTC = nextSignedTCs.get(telecomId);
				println("Sending signed request for " +
						nextSignedTC.getCiphertexts().length +
						" ciphertexts to telecom " + telecomId + "...");
				if (!telecoms.containsKey(telecomId)) {
					connectTelecom(telecomId);
					nextSignedTC.setMaxDegree(maxDegree);
				} else if (needToInformInitialOwner && telecomId == initialOwner) {
					nextSignedTC.setMaxDegree(maxDegree);
					needToInformInitialOwner = false;
				}
				try {
					writeObjectToTelecom(telecomId, nextSignedTC);
				} catch (IOException e) {
					System.err.println("Error in connection with telecom " + telecomId);
					e.printStackTrace();
					return;
				}
			}

			// Receive responses from telecoms
			investigationLists.clear();
			prevResponses.clear();
			for (int telecomId : nextSignedTCs.keySet()) {
				try {
					SignedTelecomResponse prevResponse = (SignedTelecomResponse)
							readObjectFromTelecom(telecomId);
					recordTelecomCpuTime(prevResponse.getCpuTime());
					prevResponses.put(telecomId, prevResponse);
					TelecomResponse[] telecomResponses =
							prevResponses.get(telecomId).getTelecomResponses();
					for (TelecomResponse telecomResponse : telecomResponses) {
						processTelecomResponse(telecomResponse, distance);
					}
				} catch (IOException e) {
					System.err.println("Error in connection with telecom " +
							telecomId);
					e.printStackTrace();
					return;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}
			}
		}

		// Finally, send the very last telecomResponses to our oversight agencies.
		OversightFinalResultsThread[] finalThreads =
				new OversightFinalResultsThread[oversight.length];
		for (int i = 0; i < finalThreads.length; i++) {
			finalThreads[i] = new OversightFinalResultsThread(oversight[i],
					prevResponses);
			finalThreads[i].start();
		}
		for (OversightFinalResultsThread ofrt : finalThreads) {
			try {
				ofrt.join();
			} catch (InterruptedException e) {
			} finally {
				long oCpuTime = ofrt.getOversightCpuTime();
				if (oCpuTime < 0) {
					System.err.println("Didn't get an OK from oversight agency " +
							ofrt.getAgencyId() + "!");
				} else {
					recordAgencyCpuTime(oCpuTime);
				}
				recordAgencyCpuTime(ofrt.getCpuTime());
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
