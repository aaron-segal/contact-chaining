package cc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class OversightAgency extends Agency {

	public static final String LEADER_IP = "LEADERIP";

	private Socket leaderSocket = null;
	private boolean connected = false;
	private ObjectOutputStream leaderOStream;
	private ObjectInputStream leaderIStream;

	@Override
	protected void usage() {
		System.err.println("Usage: java cc.OversightAgency config_file [-c config_file] [-d max_degree] [-i id_of_target] [-l max_length] [-k private_key_file] [-o output_path] [-q] [-s]");
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
				leaderOStream = new ObjectOutputStream(leaderSocket.getOutputStream());
				leaderIStream = new ObjectInputStream(leaderSocket.getInputStream());
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

	/**
	 * Reads a SignedTelecomResponse from the Leader and processes it.
	 * @param distance The distance remaining at this point in the search.
	 * @return True if the response validated OK, false if there was a problem.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private boolean readResponseFromLeader(int distance)
			throws ClassNotFoundException, IOException {
		// Read, verify, and process the response of the previous telecom.
		@SuppressWarnings("unchecked")
		HashMap<Integer, SignedTelecomResponse> signedResponses =
		(HashMap<Integer, SignedTelecomResponse>) leaderIStream.readObject();
		for (int telecomId : signedResponses.keySet()) {
			SignedTelecomResponse signedResponse = signedResponses.get(telecomId);
			if (!keys.verify(signedResponse)) {
				// If we failed to verify the signature, complain bitterly and quit.
				System.err.println("Failed to verify a signature on a response from "
						+ signedResponse.getTelecomId());
				return false;
			}
			for (TelecomResponse telecomResponse :
				signedResponse.getTelecomResponses()) {
				processTelecomResponse(telecomResponse, distance);
			}
		}
		// Get the degree of the initial target for timing purposes
		if (distance == 0) {
			// There is only one SignedTelecomReponse with one TelecomResponse in
			// it, in this case.
			int initialOwner = signedResponses.keySet().iterator().next();
			targetDegree = signedResponses.get(initialOwner).
					getTelecomResponses()[0].getTelecomCiphertexts().length;
		}
		return true;
	}


	/**
	 * Begin the contact chaining protocol.
	 */
	public void contactChaining() {
		super.contactChaining();
		try {
			leaderOStream.writeInt(id);
			leaderOStream.flush();
			int leaderTarget = leaderIStream.readInt();
			if (leaderTarget != targetId) {
				println("Failure: Investigating agency gave target " + leaderTarget +
						" but our target is " + targetId + ".");
				return;
			} else {
				println("Connected, targeting id " + targetId);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// Deal with target first, which requires special care.
		try {
			// Verify the initial telecom ciphertext.
			// Unlike all others, this is generated by the leader agency, not by
			// a telecom.
			SignedTelecomCiphertext signedTC =
					(SignedTelecomCiphertext) leaderIStream.readObject();
			//There should be only one signature so far, with the leader's id
			int leaderId = signedTC.getSignatures().keySet().iterator().next();
			if (!keys.verify(leaderId, signedTC)) {
				println("Failure: Investigating agency's signature (ID " + leaderId + ") does not verify!");
				return;
			}
			println("Success, signature verified");
			leaderOStream.writeObject(keys.sign(signedTC.getCiphertexts()));
			leaderOStream.flush();

			// Read telecom's response from the leader for the initial target.
			boolean readOK = readResponseFromLeader(0);
			if (!readOK) {
				return; // error text is processed in readResponseFromLeader.
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}



		// Enter main loop. We will continue until distance = maxDistance, but
		// this check has to be in the middle of the loop to mirror LeaderAgency.
		try {
			for (int distance = 1; distance <= maxDistance; distance++) {
				println("Remaining in queue: " + ciphertextsRemaining());
				// Look at what the next query to the telecoms should be and give
				// the leader a signature on those telecom ciphertexts.
				HashMap<Integer, byte[]> signatures = new HashMap<Integer, byte[]>();
				for (int telecomId : investigationLists.keySet()) {
					signatures.put(telecomId, keys.sign(getCiphertexts(telecomId)));
				}
				leaderOStream.writeObject(signatures);
				leaderOStream.flush();

				investigationLists.clear();
				// Get a response from the telecoms via the leader agency.
				boolean readOK = readResponseFromLeader(distance);
				if (!readOK) {
					return; // error text is processed in readResponseFromLeader.
				}
			}

			// If we get to this point, we're done with the main loop.
			// Tell the leader how much CPU time we spent on this.
			leaderOStream.writeLong(bean.getCurrentThreadCpuTime());
			leaderOStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Closes stream to the leader.
	 */
	public void closeAll() {
		try {
			leaderOStream.close();
			leaderIStream.close();
			leaderSocket.close();
		} catch (IOException e) {}
	}

	public static void main(String[] args) {
		OversightAgency oAgency = new OversightAgency(args);
		oAgency.contactChaining();
		oAgency.writeOutput();
		oAgency.reportTiming();
		oAgency.closeAll();
	}
}
